package com.example.budgetplanner.ui2.recurring

import android.app.Application
import androidx.lifecycle.*
import com.example.budgetplanner.BudgetApplication
import com.example.budgetplanner.data.local.entities.RecurringExpenseEntity
import com.example.budgetplanner.data.repository.RecurringRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId

data class RecurringUi(
    val month: YearMonth = YearMonth.now(),
    val rateEurRon: Double = 5.00,
    // name -> (eur, ron)
    val rows: Map<String, Pair<Double?, Double?>> = emptyMap(),
    val totalRon: Double = 0.0
)

class RecurringViewModel(app: Application) : AndroidViewModel(app) {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val repo: RecurringRepository =
        (app as BudgetApplication).recurringRepository   // ✅ now resolves

    private val _ui = MutableStateFlow(RecurringUi())
    val ui: StateFlow<RecurringUi> = _ui.asStateFlow()

    private val names = listOf("RENT","GAZ","CURENT","DIGI","INTRETINERE")

    init { loadMonth(_ui.value.month) }

    fun prevMonth() = loadMonth(_ui.value.month.minusMonths(1))
    fun nextMonth() = loadMonth(_ui.value.month.plusMonths(1))

    private fun loadMonth(m: YearMonth) = viewModelScope.launch {
        // one-shot read for now
        val items: List<RecurringExpenseEntity> = repo.observeMonth(m, zone).first()
        val map: Map<String, Pair<Double?, Double?>> =
            names.associateWith { n ->
                val e = items.find { it.name == n }
                (e?.amountEur) to (e?.amountRon)
            }
        val total = repo.totalRon(m, zone)
        _ui.update { it.copy(month = m, rows = map, totalRon = total) }
    }

    fun updateRate(rate: Double) {
        _ui.update { it.copy(rateEurRon = rate) }
    }

    fun setEur(name: String, eurStr: String) {
        val rate = _ui.value.rateEurRon
        val eur = eurStr.toDoubleOrNull()
        val ron = eur?.let { it * rate }
        _ui.update { it.copy(rows = it.rows + (name to (eur to ron))) }
    }

    fun setRon(name: String, ronStr: String) {
        val rate = _ui.value.rateEurRon
        val ron = ronStr.toDoubleOrNull()
        val eur = ron?.let { if (rate != 0.0) it / rate else null }
        _ui.update { it.copy(rows = it.rows + (name to (eur to ron))) }
    }

    fun saveRow(name: String) = viewModelScope.launch {
        val (eur, ron) = _ui.value.rows[name] ?: (null to null)
        val rate = _ui.value.rateEurRon
        repo.save(_ui.value.month, zone, name, eur, ron, rate)
        val total = repo.totalRon(_ui.value.month, zone)
        _ui.update { it.copy(totalRon = total) }
    }
}

class RecurringVMFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecurringViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecurringViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown VM class")
    }
}
