package com.example.budgetplanner.ui2.transactions


import android.app.Application
import androidx.lifecycle.*
import com.example.budgetplanner.BudgetApplication
import com.example.budgetplanner.domain.model.Transaction
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Job
import java.time.*
import kotlin.math.abs
import com.example.budgetplanner.domain.model.Category
import com.example.budgetplanner.domain.model.Source

data class TxRowUi(val tx: Transaction)
data class DayGroup(val date: LocalDate, val items: List<TxRowUi>)

data class TransactionsUiState(
    val month: YearMonth = YearMonth.now(),
    val rows: List<DayGroup> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selected: Transaction? = null            // <-- for bottom sheet
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
            repo.observeMonth(m, zone).collect { list ->
                _ui.update { it.copy(rows = groupForUi(list), isLoading = false) }
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
    fun onRowClick(t: Transaction) { _ui.update { it.copy(selected = t) } }
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
    }


class TransactionsVMFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return TransactionsViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown VM class")
    }
}