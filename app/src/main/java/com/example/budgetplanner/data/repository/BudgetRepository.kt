package com.example.budgetplanner.data.repository


import com.example.budgetplanner.data.local.entities.MerchantRuleEntity
import com.example.budgetplanner.data.local.entities.SavingsEntity
import com.example.budgetplanner.data.local.entities.TransactionEntity
import com.example.budgetplanner.data.local.entities.dao.MerchantRuleDao
import com.example.budgetplanner.data.local.entities.dao.ReimbursementLinkDao
import com.example.budgetplanner.data.local.entities.dao.TransactionDao
import com.example.budgetplanner.domain.model.Category
import com.example.budgetplanner.domain.model.Transaction
import com.example.budgetplanner.ui2.transactions.CreditAllocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.YearMonth
import java.time.ZoneId


class BudgetRepository(
    private val transactionDao: TransactionDao,
    private val reimbursementLinkDao: ReimbursementLinkDao,
    private val savingsDao: com.example.budgetplanner.data.local.entities.dao.SavingsDao,
    private val merchantRuleDao: MerchantRuleDao
) {
    fun observeMonth(yearMonth: YearMonth, zoneId: ZoneId = ZoneId.systemDefault()): Flow<List<Transaction>> {
        val start = yearMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return transactionDao.observeBetween(start, end).map { it.map { e -> e.toDomain() } }
    }

    // Stub for now — later this will call your backend OpenBanking endpoint
    suspend fun syncMonthFromBank(@Suppress("UNUSED_PARAMETER") yearMonth: YearMonth): Result<Unit> {
        return Result.success(Unit)
    }

    suspend fun addTransaction(tx: com.example.budgetplanner.domain.model.Transaction) {
        transactionDao.insert(tx.toEntity())
    }

    suspend fun deleteTransactionById(id: Long) =
        transactionDao.deleteById(id)

    suspend fun updateTransactionNoteAndCategory(id: Long, note: String?, category: String) =
        transactionDao.updateNoteAndCategory(id, note, category)

    suspend fun setExcludePersonal(id: Long, exclude: Boolean) =
        transactionDao.setExcludePersonal(id, exclude)

    suspend fun setParty(id: Long, party: String?) =
        transactionDao.setParty(id, party)

    suspend fun applyMomReimbursementForCredit(
        creditId: Long,
        windowDays: Int = 14
    ): Triple<Double, Int, Double> {
        val credit = transactionDao.getById(creditId) ?: return Triple(0.0, 0, 0.0)
        require(credit.originalAmount > 0) { "Credit must be positive" }

        val creditRemainingStart = kotlin.math.abs(credit.amountRon)
        var remaining = creditRemainingStart - reimbursementLinkDao.sumAllocatedFromCredit(creditId)
        if (0.0 >= remaining) {
            return Triple(0.0, 0, 0.0)
        }

        val creditDay = java.time.Instant.ofEpochMilli(credit.timestamp)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val from = creditDay.minusDays(windowDays.toLong()).atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val to = credit.timestamp

        val candidates = transactionDao.getMomExpenseCandidates(from, to)
        var matched = 0.0
        var count = 0

        for (e in candidates) {
            if (0.0 >= remaining) break
            val needTotal = kotlin.math.abs(e.amountRon)
            val already = reimbursementLinkDao.sumCoveredForExpense(e.id)
            val need = (needTotal - already).coerceAtLeast(0.0)
            if (need <= 0.0) continue

            val cover = kotlin.math.min(need, remaining)
            if (cover > 0) {
                reimbursementLinkDao.insert(
                    com.example.budgetplanner.data.local.entities.ReimbursementLinkEntity(
                        expenseTxId = e.id, creditTxId = creditId, amountRon = cover
                    )
                )
                matched += cover
                remaining -= cover
                if (cover == need) {
                    // nice UX: mark as fully reimbursed
                    transactionDao.update(
                        e.copy(reimbursedGroup = "MOM")
                    )
                }
                count++
            }
        }

        val surplus = remaining.coerceAtLeast(0.0)
        return Triple(matched, count, surplus)
    }

    suspend fun applyMomReimbursementForExpense(
        expenseId: Long,
        windowDays: Int = 14
    ): Pair<Double, Double> {
        val e = transactionDao.getById(expenseId) ?: return 0.0 to 0.0
        require(e.originalAmount < 0) { "Expense must be negative" }

        val needTotal = kotlin.math.abs(e.amountRon)
        val already = reimbursementLinkDao.sumCoveredForExpense(expenseId)
        var uncovered = (needTotal - already).coerceAtLeast(0.0)
        if (uncovered <= 0.0) return needTotal to 0.0

        // find credits from MOM in window
        val zone = java.time.ZoneId.systemDefault()
        val from = java.time.Instant.ofEpochMilli(e.timestamp).atZone(zone).toLocalDate()
            .minusDays(windowDays.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
        val credits = observeCreditsFromMom(from, e.timestamp) // helper below

        var allocated = 0.0
        for (c in credits) {
            if (uncovered <= 0.0) {
                break
            }
            val remaining = c.amountRon - reimbursementLinkDao.sumAllocatedFromCredit(c.id)
            if (remaining <= 0.0) continue
            val cover = kotlin.math.min(uncovered, remaining)
            reimbursementLinkDao.insert(
                com.example.budgetplanner.data.local.entities.ReimbursementLinkEntity(
                    expenseTxId = expenseId, creditTxId = c.id, amountRon = cover
                )
            )
            allocated += cover
            uncovered -= cover
            if (uncovered <= 0.0) {
                transactionDao.update(e.copy(reimbursedGroup = "MOM"))
                break
            }
        }
        return allocated to uncovered
    }

    suspend fun allocateSurplusToSavings(surplus: Double, potName: String = "Mom surplus") {
        val existing = savingsDao.getByName(potName)
        if (existing == null) {
            savingsDao.insert(SavingsEntity(name = potName, amountRon = surplus))
        } else {
            savingsDao.updateAmount(existing.id, existing.amountRon + surplus)
        }
    }



    // small internal: credits from MOM in a window (newest first)
    private suspend fun observeCreditsFromMom(fromMillis: Long, toMillis: Long): List<com.example.budgetplanner.data.local.entities.TransactionEntity> {
        // quick ad-hoc query via existing DAO (add if you want a dedicated @Query)
        // For simplicity, reuse getMomExpenseCandidates but invert filters:
        // We'll add a simple helper in TransactionDao:
        return transactionDao.getMomCredits(fromMillis, toMillis)
    }

    // expose mom credits within a window

    // sums for the bottom sheet
    suspend fun sumCoveredForExpense(expenseId: Long) =
        reimbursementLinkDao.sumCoveredForExpense(expenseId)

    suspend fun sumAllocatedFromCredit(creditId: Long) =
        reimbursementLinkDao.sumAllocatedFromCredit(creditId)

    suspend fun getLinksForCredit(creditId: Long) =
        reimbursementLinkDao.getLinksForCredit(creditId)

    // reuse you already added earlier:
    suspend fun getMomCredits(fromMillis: Long, toMillis: Long) =
        transactionDao.getMomCredits(fromMillis, toMillis)

    // personal spend = only negative, not excluded
    suspend fun personalSpendForMonth(month: YearMonth, zone: ZoneId): Double {
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        // Fast path: let Room sum it (recommended). If you don’t want a new DAO,
        // you can fallback to loading and summing in memory (shown below).
        return transactionDao.sumPersonalSpendBetween(start, end)
    }

    // open for mom = sum over excluded expenses of (abs(amount) - covered)
    suspend fun openForMomForMonth(month: YearMonth, zone: ZoneId): Double {
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val excluded = transactionDao.getExcludedExpensesBetween(start, end)
        var totalOpen = 0.0
        for (e in excluded) {
            val total = kotlin.math.abs(e.amountRon)
            val covered = reimbursementLinkDao.sumCoveredForExpense(e.id)
            val open = (total - covered).coerceAtLeast(0.0)
            totalOpen += open
        }
        return totalOpen
    }

    suspend fun personalSpendByCategory(
        month: YearMonth,
        zone: ZoneId = ZoneId.systemDefault()
    ): Map<Category, Double> {
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end   = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

        // pull entities and map to domain
        val txs = transactionDao.getBetween(start, end).map { it.toDomain() }

        val result = mutableMapOf<Category, Double>()
        for (t in txs) {
            if (t.originalAmount >= 0) continue          // only expenses
            if (t.excludePersonal) continue              // ignore excluded

            // subtract what Mom already covered for this expense
            val covered = reimbursementLinkDao.sumCoveredForExpense(t.id)
            val open = (kotlin.math.abs(t.amountRon) - covered).coerceAtLeast(0.0)
            if (open <= 0.0) continue

            val cat = t.category                          // already Category enum
            result[cat] = (result[cat] ?: 0.0) + open
        }
        return result
    }

    // --- Savings ---
    fun observeSavings() = savingsDao.observeAll()
    // ---- Savings
    fun observeSavingsTotalRon(): kotlinx.coroutines.flow.Flow<Double?> =
        savingsDao.observeTotalRon()

    fun observeNetTxSum(): kotlinx.coroutines.flow.Flow<Double> =
        transactionDao.observeNetSum()
    suspend fun ensureSavingsPot(name: String) {
        if (savingsDao.getByName(name) == null) {
            savingsDao.insert(com.example.budgetplanner.data.local.entities.SavingsEntity(name = name, amountRon = 0.0))
        }
    }

    fun observePersonalSpendForMonth(
        ym: java.time.YearMonth,
        zone: java.time.ZoneId
    ): kotlinx.coroutines.flow.Flow<Double> {
        val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end   = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return transactionDao.observePersonalSpendBetween(start, end)
    }

    suspend fun addSavings(name: String, amountRon: Double) {
        val existing = savingsDao.getByName(name)
        if (existing == null) {
            savingsDao.insert(com.example.budgetplanner.data.local.entities.SavingsEntity(name = name, amountRon = amountRon))
        } else {
            savingsDao.updateAmount(existing.id, existing.amountRon + amountRon)
        }
    }

    suspend fun setSavingsAmount(id: Long, amountRon: Double) =
        savingsDao.updateAmount(id, amountRon)

    suspend fun deleteSavings(id: Long) =
        savingsDao.deleteById(id)

    private enum class MatchType { CONTAINS, STARTS_WITH, EXACT }

    private fun matches(merchant: String, rule: MerchantRuleEntity): Boolean {
        val m = merchant.uppercase()
        val p = rule.pattern.uppercase()
        return when (MatchType.valueOf(rule.matchType)) {
            MatchType.CONTAINS    -> m.contains(p)
            MatchType.STARTS_WITH -> m.startsWith(p)
            MatchType.EXACT       -> m == p
        }
    }

    /**
     * Apply rules to *this month*:
     * - sets category if rule.category != null
     * - sets excludePersonal if true
     * - sets party if provided
     */
    suspend fun applyRulesToMonth(month: YearMonth, zone: ZoneId): Result<Int> = withContext(
        Dispatchers.IO) {
        val dao = merchantRuleDao
        val rules = dao.getAll()
        if (rules.isEmpty()) return@withContext Result.success(0)

        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val txs = transactionDao.getBetween(start, end)   // make sure you have getBetween()

        var touched = 0
        for (t in txs) {
            val merchant = t.merchant ?: continue
            for (r in rules) {
                if (!matches(merchant, r)) continue

                var changed = false
                if (r.category != null && r.category != t.category) {
                    transactionDao.updateNoteAndCategory(t.id, t.note, r.category)
                    changed = true
                }
                if (r.excludePersonal && !t.excludePersonal) {
                    transactionDao.setExcludePersonal(t.id, true)
                    changed = true
                }
                if (r.setParty != null && r.setParty != t.party) {
                    transactionDao.setParty(t.id, r.setParty)
                    changed = true
                }
                if (changed) touched++
                break // first matching rule wins
            }
        }
        Result.success(touched)
    }

    /** Seed a few defaults (only if the table is empty). */

    val list = listOf(
        // Supermarkets → FOOD
        MerchantRuleEntity(matchType = "CONTAINS", pattern = "MEGA",       category = "FOOD", priority = 10),
        MerchantRuleEntity(matchType = "CONTAINS", pattern = "CARREFOUR",  category = "FOOD", priority = 10),
        MerchantRuleEntity(matchType = "CONTAINS", pattern = "KAUFLAND",   category = "FOOD", priority = 10),
        MerchantRuleEntity(matchType = "CONTAINS", pattern = "LIDL",       category = "FOOD", priority = 10),
        MerchantRuleEntity(matchType = "CONTAINS", pattern = "PROFI",      category = "FOOD", priority = 10),
        MerchantRuleEntity(matchType = "CONTAINS", pattern = "PENNY",      category = "FOOD", priority = 10),
        MerchantRuleEntity(matchType = "CONTAINS", pattern = "DIANA",      category = "FOOD", priority = 10),
        MerchantRuleEntity(matchType = "CONTAINS", pattern = "ANNABELLA",      category = "FOOD", priority = 10),




        // Pharmacies → FARMACY and exclude from personal spend
        MerchantRuleEntity(matchType = "CONTAINS", pattern = "CATENA",     category = "FARMACY", excludePersonal = true, priority = 20),
        MerchantRuleEntity(matchType = "CONTAINS", pattern = "SENSIBLU",   category = "FARMACY", excludePersonal = true, priority = 20),
        MerchantRuleEntity(matchType = "CONTAINS", pattern = "HELP NET",   category = "FARMACY", excludePersonal = true, priority = 20),
        MerchantRuleEntity(matchType = "CONTAINS", pattern = "DR.MAX",     category = "FARMACY", excludePersonal = true, priority = 20),
        MerchantRuleEntity(matchType = "CONTAINS", pattern = "BAJAN",      category = "FARMACY", excludePersonal = true, priority = 20),


        // Transport examples
        MerchantRuleEntity(matchType = "CONTAINS", pattern = "STB",        category = "TRANSPORT", priority = 30),
        MerchantRuleEntity(matchType = "CONTAINS", pattern = "METROREX",   category = "TRANSPORT", priority = 30),
        MerchantRuleEntity(matchType = "CONTAINS", pattern = "UBER",       category = "TRANSPORT", priority = 30),
        MerchantRuleEntity(matchType = "CONTAINS", pattern = "BOLT",       category = "TRANSPORT", priority = 30),

        // Incoming transfers from Mom (example)
        MerchantRuleEntity(matchType = "CONTAINS", pattern = "MONICA ELENA LITA",       category = "OTHER", setParty = "MOM", priority = 5),
    )
    suspend fun seedDefaultRulesIfEmpty() = withContext(Dispatchers.IO) {
        val dao = merchantRuleDao
        if (dao.count() > 0) return@withContext


        list.forEach { dao.insert(it) }
    }

    // ---------- helper: safe enum from String ----------
    private fun String?.toCategoryOrNull(): Category? =
        runCatching { if (this.isNullOrBlank()) null else Category.valueOf(this) }.getOrNull()

    suspend fun recategorizeMonth(month: YearMonth, zone: ZoneId) {
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end   = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val rules: List<MerchantRuleEntity> = merchantRuleDao.getAll()  // <-- ‘rules’ is here
        if (rules.isEmpty()) return

        val rows: List<TransactionEntity> = transactionDao.getBetween(start, end)

        for (e in rows) {
            val merchant = (e.merchant ?: "").uppercase()
            var updated = e
            var changed = false

            for (r in rules) {
                val pat = r.pattern.uppercase()
                val matches = when (r.matchType) {
                    "EXACT"       -> merchant == pat
                    "STARTS_WITH" -> merchant.startsWith(pat)
                    else          -> merchant.contains(pat) // CONTAINS
                }
                if (!matches) continue

                r.category.toCategoryOrNull()?.let { cat ->
                    if (e.category != cat.name) {
                        updated = updated.copy(category = cat.name)
                        changed = true
                    }
                }
                if (r.excludePersonal && !e.excludePersonal) {
                    updated = updated.copy(excludePersonal = true)
                    changed = true
                }
                r.setParty?.let { p ->
                    if (e.party != p) {
                        updated = updated.copy(party = p)
                        changed = true
                    }
                }
                if (changed) break // first match wins
            }

            if (changed) transactionDao.update(updated)
        }
    }

    private val MOM_POT = "Mom surplus"

    suspend fun markCreditFromMom(creditId: Long): CreditAllocation { // returns what happened, optional
        // 1) tag as MOM, in case it wasn't
        transactionDao.setParty(creditId, "MOM")

        // 2) allocate to expenses in the look-back window; capture surplus
        val (allocated, count, surplus) = applyMomReimbursementForCredit(creditId)

        // 3) if extra money remained, push it to the savings pot
        if (surplus > 0.0) {
            allocateSurplusToSavings(surplus, MOM_POT)
        }
        return CreditAllocation(allocated = allocated, items = count, surplus = surplus)
    }

    // Choose the right field from your MerchantRuleEntity (e.g., merchant / vendor / alias / pattern / name)
    suspend fun vendorSuggestions(): List<String> {
        // If you also keep a predefined `list: List<String>`, merge or return that instead.
        return merchantRuleDao.getAll().map { it.pattern /* or it.vendor or it.name */ }
        // or: return list  // if you already have the hardcoded vendor list you mentioned
    }

}