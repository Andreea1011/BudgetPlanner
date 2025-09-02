package com.example.budgetplanner.data.repository


import com.example.budgetplanner.data.local.entities.dao.TransactionDao
import com.example.budgetplanner.data.local.entities.dao.ReimbursementLinkDao
import com.example.budgetplanner.data.local.entities.dao.SavingsDao   // if you’re using savings now
import com.example.budgetplanner.data.local.entities.ReimbursementLinkEntity
import com.example.budgetplanner.data.local.entities.TransactionEntity
import com.example.budgetplanner.data.local.entities.SavingsEntity
import com.example.budgetplanner.domain.model.Category
import com.example.budgetplanner.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.*

class BudgetRepository(
    private val transactionDao: TransactionDao,
    private val reimbursementLinkDao: ReimbursementLinkDao,
    private val savingsDao: com.example.budgetplanner.data.local.entities.dao.SavingsDao // non-null
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
        val existing = savingsDao?.getByName(potName)
        if (existing == null) {
            val id = savingsDao?.insert(com.example.budgetplanner.data.local.entities.SavingsEntity(name = potName, amountRon = surplus))
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
    fun observeSavingsTotalRon() = savingsDao.observeTotalRon()

    suspend fun ensureSavingsPot(name: String) {
        if (savingsDao.getByName(name) == null) {
            savingsDao.insert(com.example.budgetplanner.data.local.entities.SavingsEntity(name = name, amountRon = 0.0))
        }
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

}