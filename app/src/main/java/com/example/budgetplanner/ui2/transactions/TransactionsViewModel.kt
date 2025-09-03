package com.example.budgetplanner.ui2.transactions

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.app.Application
import androidx.lifecycle.*
import com.example.budgetplanner.BudgetApplication
import com.example.budgetplanner.domain.model.Transaction
import kotlinx.coroutines.flow.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.budgetplanner.domain.model.Category
import com.example.budgetplanner.domain.model.Source
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetplanner.ui2.transactions.TransactionsVMFactory
import com.example.budgetplanner.ui2.transactions.TransactionsViewModel



data class TxRowUi(val tx: Transaction)
data class DayGroup(val date: LocalDate, val items: List<TxRowUi>)
data class TransactionsUiState(
    val month: YearMonth = YearMonth.now(),
    val rows: List<DayGroup> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selected: Transaction? = null,

    // NEW:
    val expenseCoverage: ExpenseCoverage? = null,
    val creditAllocation: CreditAllocation? = null,

    val totalPersonalSpend: Double = 0.0,
    val totalOpenForMom: Double = 0.0
)
class TransactionsViewModel(private val app: Application) : AndroidViewModel(app) {
    private val repo get() = (app as BudgetApplication).repository
    private val zone: ZoneId = ZoneId.systemDefault()

    private val _ui = MutableStateFlow(TransactionsUiState())
    val ui: StateFlow<TransactionsUiState> = _ui.asStateFlow()

    private var monthJob: kotlinx.coroutines.Job? = null

    init { selectMonth(_ui.value.month) }

    fun prevMonth() = selectMonth(_ui.value.month.minusMonths(1))
    fun nextMonth() = selectMonth(_ui.value.month.plusMonths(1))

    fun sync() = viewModelScope.launch {
        _ui.update { it.copy(isLoading = true, error = null) }
        val res = repo.syncMonthFromBank(_ui.value.month)
        _ui.update { it.copy(isLoading = false, error = res.exceptionOrNull()?.message) }
    }

    private fun selectMonth(m: YearMonth) {
        _ui.update { it.copy(month = m, isLoading = true, error = null) }
        monthJob?.cancel()
        monthJob = viewModelScope.launch {
            try {
                // list
                launch {
                    repo.observeMonth(m, zone).collectLatest { list ->
                        _ui.update { it.copy(rows = groupForUi(list), isLoading = false) }
                    }
                }
                // totals
                launch {
                    val personal = repo.personalSpendForMonth(m, zone)
                    val openMom = repo.openForMomForMonth(m, zone)
                    _ui.update { it.copy(totalPersonalSpend = personal, totalOpenForMom = openMom) }
                }
            } catch (t: Throwable) {
                _ui.update { it.copy(isLoading = false, error = t.message) }
            }
        }
    }

    private fun groupForUi(list: List<Transaction>): List<DayGroup> {
        val byDate = list.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
        return byDate.toSortedMap(compareByDescending { it }).map { (date, txs) ->
            DayGroup(date, txs.map { TxRowUi(it) })
        }
    }

    // ---------- row actions ----------
    fun onRowClick(t: Transaction) {
        _ui.update { it.copy(selected = t, expenseCoverage = null, creditAllocation = null) }
        viewModelScope.launch {
            if (t.originalAmount < 0) {
                val total = kotlin.math.abs(t.amountRon)
                val covered = repo.sumCoveredForExpense(t.id)
                _ui.update { it.copy(expenseCoverage = ExpenseCoverage(total, covered)) }
            } else if (t.originalAmount > 0) {
                val allocated = repo.sumAllocatedFromCredit(t.id)
                val links = repo.getLinksForCredit(t.id)
                val surplus = (t.amountRon - allocated).coerceAtLeast(0.0)
                _ui.update { it.copy(creditAllocation = CreditAllocation(allocated, links.size, surplus)) }
            }
        }
    }

    fun dismissSheet() { _ui.update { it.copy(selected = null) } }

    fun deleteSelected() {
        val t = _ui.value.selected ?: return
        viewModelScope.launch {
            repo.deleteTransactionById(t.id)
            dismissSheet()
        }
    }

    fun saveSelected(note: String?, category: Category) {
        val t = _ui.value.selected ?: return
        viewModelScope.launch {
            repo.updateTransactionNoteAndCategory(t.id, note, category.name)
            dismissSheet()
        }
    }

    // ---------- debug: seed a few rows ----------
    fun seedDemo() = viewModelScope.launch {
        val now = LocalDate.now()
        suspend fun add(dayOffset: Long, amount: Double, cur: String, merchant: String, cat: Category) {
            val ts = now.minusDays(dayOffset).atStartOfDay(zone).toInstant().toEpochMilli()
            val tx = Transaction(
                id = 0L,
                timestamp = ts,
                originalAmount = amount,
                originalCurrency = cur,
                merchant = merchant,
                note = null,
                category = cat,
                source = Source.MANUAL,
                amountRon = amount,
                pending = false
            )
            repo.addTransaction(tx)
        }
        add(0, -49.99, "RON", "MEGA IMAGE", Category.FOOD)
        add(1, -12.50, "RON", "STB TICKET", Category.TRANSPORT)
        add(2, +5000.00, "RON", "Salary", Category.OTHER)
        add(5, -83.40, "RON", "Decathlon", Category.DORINTE)
        add(7, -350.00, "EUR", "Rent", Category.APARTMENT)
    }

    fun formatSigned(a: Double, cur: String): String {
        val sign = if (a < 0) "-" else "+"
        val abs = kotlin.math.abs(a)
        return "$sign${"%.2f".format(Locale.getDefault(), abs)} $cur"
    }

    fun formatHeader(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("dd MMMM, EEEE", Locale.getDefault()))

    fun seedMomScenario() = viewModelScope.launch {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()

        suspend fun add(d: Int, amt: Double, merch: String, note: String? = null) {
            val ts = today.atStartOfDay(zone).plusDays(d.toLong())
                .toInstant().toEpochMilli()
            val tx = com.example.budgetplanner.domain.model.Transaction(
                id = 0L,
                timestamp = ts,
                originalAmount = amt,           // negative = expense, positive = credit
                originalCurrency = "RON",
                merchant = merch,
                note = note,
                category = com.example.budgetplanner.domain.model.Category.FOOD,
                source = com.example.budgetplanner.domain.model.Source.MANUAL,
                amountRon = amt,                // keep 1:1 for testing
                pending = false
            )
            repo.addTransaction(tx)
        }

        // 2 grocery expenses (for Mom)
        add(0, -40.00, "Carrefour Rahova", "groceries")
        add(0, -25.50, "Mega Image", "groceries")

        // one of your own expenses (should stay in personal spend)
        add(0, -19.99, "McDonalds", "my lunch")

        // Mom reimburses later the same day with a little extra (+70)
        add(0, +70.00, "Transfer", "from mom")
}

    fun dumpMomLinksToLog() = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val from = now - 30L * 24 * 3600 * 1000
        val credits = repo.getMomCredits(from, now)         // ✅ wrapper
        credits.forEach { c ->
            val links = repo.getLinksForCredit(c.id)        // ✅ wrapper
            android.util.Log.d("MOM", "credit id=${c.id}, amt=${c.amountRon}, links=$links")
        }
    }

    fun setExcludePersonal(id: Long, exclude: Boolean) = viewModelScope.launch {
        val t = ui.value.selected ?: return@launch
        // Guard: only expenses can be excluded
        if (t.originalAmount >= 0) return@launch

        repo.setExcludePersonal(id, exclude)
        if (exclude) {
            // Only attempts matching for expenses
            runCatching { repo.applyMomReimbursementForExpense(id) }
        }
        refreshMonthTotals()
    }

    fun setParty(id: Long, party: String?) = viewModelScope.launch {
        val t = ui.value.selected ?: return@launch
        // Guard: only credits can be marked as From Mom
        if (party == "MOM" && t.originalAmount <= 0) return@launch

        repo.setParty(id, party)
        if (party == "MOM") {
            runCatching { repo.applyMomReimbursementForCredit(id) }
        }
        refreshMonthTotals()
    }

    private suspend fun refreshMonthTotals() {
        val m = _ui.value.month
        val personal = repo.personalSpendForMonth(m, zone)
        val openMom  = repo.openForMomForMonth(m, zone)
        _ui.update { it.copy(totalPersonalSpend = personal, totalOpenForMom = openMom) }
    }

    fun autoLabel() = viewModelScope.launch {
        val zone = ZoneId.systemDefault()
        repo.applyRulesToMonth(_ui.value.month, zone)
        // the list will refresh automatically because you're observing the month from Room
    }


}

data class ExpenseCoverage(val total: Double, val covered: Double) {
    val open: Double get() = (total - covered).coerceAtLeast(0.0)
}
data class CreditAllocation(val allocated: Double, val items: Int, val surplus: Double)



class TransactionsVMFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionsViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown VM class")
    }
}
