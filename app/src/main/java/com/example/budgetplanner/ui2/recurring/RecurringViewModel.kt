package com.example.budgetplanner.ui2.recurring

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.budgetplanner.BudgetApplication
import com.example.budgetplanner.data.local.entities.RecurringExpenseEntity
import com.example.budgetplanner.data.repository.RateRepository
import com.example.budgetplanner.data.repository.RecurringRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.round

// --- UI STATE (rows = Map<String, Pair<EUR?, RON?>>) ---
data class RecurringUi(
    val month: YearMonth = YearMonth.now(),
    val rateEurRon: Double = 5.00,
    val rows: Map<String, Pair<Double?, Double?>> = emptyMap(),
    val totalRon: Double = 0.0,
    val error: String? = null
)

class RecurringViewModel(app: Application) : AndroidViewModel(app) {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val repo: RecurringRepository = (app as BudgetApplication).recurringRepository
    private val rateRepo: RateRepository = (app as BudgetApplication).rateRepository

    private val _ui = MutableStateFlow(RecurringUi())
    val ui: StateFlow<RecurringUi> = _ui.asStateFlow()

    private val names = listOf("RENT", "GAZ", "CURENT", "DIGI", "INTRETINERE")

    init { loadMonth(_ui.value.month) }

    fun prevMonth() = loadMonth(_ui.value.month.minusMonths(1))
    fun nextMonth() = loadMonth(_ui.value.month.plusMonths(1))

    private fun loadMonth(m: YearMonth) = viewModelScope.launch {
        val items: List<RecurringExpenseEntity> = repo.observeMonth(m, zone).first()
        val map: Map<String, Pair<Double?, Double?>> =
            names.associateWith { n ->
                val e = items.find { it.name == n }
                (e?.amountEur) to (e?.amountRon)
            }
        val total = repo.totalRon(m, zone)
        _ui.update { it.copy(month = m, rows = map, totalRon = total, error = null) }
    }

    fun updateRate(rate: Double) {
        _ui.update { it.copy(rateEurRon = rate) }
        // No mass recalculation here—screen mirrors locally while typing.
    }

    /** When user types in EUR, update local rows (and recompute RON for that row). */
    fun setEur(name: String, value: Double?) {
        _ui.update { s ->
            val old = s.rows[name] ?: (null to null)
            val newRon = value?.let { round2(it * s.rateEurRon) }
            val newRows = s.rows.toMutableMap().apply { put(name, value to newRon) }
            s.copy(rows = newRows, totalRon = totalRon(newRows))
        }
    }

    /** When user types in RON, update local rows (and recompute EUR for that row). */
    fun setRon(name: String, value: Double?) {
        _ui.update { s ->
            val old = s.rows[name] ?: (null to null)
            val newEur = value?.let { if (s.rateEurRon > 0) round2(it / s.rateEurRon) else old.first }
            val newRows = s.rows.toMutableMap().apply { put(name, newEur to value) }
            s.copy(rows = newRows, totalRon = totalRon(newRows))
        }
    }

    /** Save based on what’s currently typed in the UI text fields. */
    fun saveRowImmediate(name: String, eurText: String, ronText: String) = viewModelScope.launch {
        val eur = eurText.toDoubleOrNull()
        val ron = ronText.toDoubleOrNull()
        val rate = _ui.value.rateEurRon
        repo.save(_ui.value.month, zone, name, eur, ron, rate)

        // Reload to reflect Room truth + recompute totals.
        loadMonth(_ui.value.month)
    }

    fun fetchRate() = viewModelScope.launch {
        rateRepo.latestEurRon()
            .onSuccess { r -> _ui.update { it.copy(rateEurRon = r, error = null) } }
            .onFailure { e -> _ui.update { it.copy(error = e.message ?: "Rate fetch failed") } }
    }

    private fun totalRon(map: Map<String, Pair<Double?, Double?>>): Double =
        map.values.sumOf { it.second ?: 0.0 }

    private fun round2(x: Double) = round(x * 100.0) / 100.0
}

// Factory
class RecurringVMFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecurringViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecurringViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown VM class")
    }
}
