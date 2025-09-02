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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId

/* ----------------- UI models ----------------- */

enum class Edited { NONE, EUR, RON }

data class RecurringRowUi(
    val name: String,
    val eur: Double?,     // nullable if empty
    val ron: Double?,     // nullable if empty
    val edited: Edited = Edited.NONE
)

data class RecurringUiState(
    val month: YearMonth = YearMonth.now(),
    val rateEurRon: Double = 5.0,
    val rows: Map<String, RecurringRowUi> = emptyMap(),
    val totalRon: Double = 0.0,
    val error: String? = null
)

/* ----------------- ViewModel ----------------- */

class RecurringViewModel(app: Application) : AndroidViewModel(app) {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val repo: RecurringRepository =
        (app as BudgetApplication).recurringRepository
    private val rateRepository: RateRepository =
        (app as BudgetApplication).rateRepository

    private val names = listOf("RENT", "GAZ", "CURENT", "DIGI", "INTRETINERE")

    private val _ui = MutableStateFlow(
        RecurringUiState(
            rows = defaultRows(),
            rateEurRon = 5.0
        ).recalcTotal()
    )
    val ui: StateFlow<RecurringUiState> = _ui

    init {
        loadMonth(_ui.value.month)
    }

    fun prevMonth() = loadMonth(_ui.value.month.minusMonths(1))
    fun nextMonth() = loadMonth(_ui.value.month.plusMonths(1))

    private fun loadMonth(m: YearMonth) = viewModelScope.launch {
        val items: List<RecurringExpenseEntity> = repo.observeMonth(m, zone).first()
        val rows: Map<String, RecurringRowUi> = names.associateWith { n ->
            val e = items.find { it.name == n }
            RecurringRowUi(name = n, eur = e?.amountEur, ron = e?.amountRon, edited = Edited.NONE)
        }
        val total = repo.totalRon(m, zone)
        _ui.update { it.copy(month = m, rows = rows, totalRon = total, error = null) }
    }

    /* ----- user edits ----- */

    fun setEur(name: String, value: Double?) {
        _ui.update { s ->
            val row = s.rows[name] ?: return
            val newRon = value?.let { (it * s.rateEurRon).round2() }
            val newRow = row.copy(eur = value, ron = newRon, edited = Edited.EUR)
            s.copy(rows = s.rows + (name to newRow)).recalcTotal()
        }
    }

    fun setRon(name: String, value: Double?) {
        _ui.update { s ->
            val row = s.rows[name] ?: return
            val newEur = value?.let { if (s.rateEurRon > 0) (it / s.rateEurRon).round2() else row.eur }
            val newRow = row.copy(ron = value, eur = newEur, edited = Edited.RON)
            s.copy(rows = s.rows + (name to newRow)).recalcTotal()
        }
    }

    fun saveRow(name: String) = viewModelScope.launch {
        val row = _ui.value.rows[name]
        val eur = row?.eur
        val ron = row?.ron
        val rate = _ui.value.rateEurRon
        repo.save(_ui.value.month, zone, name, eur, ron, rate)
        val total = repo.totalRon(_ui.value.month, zone)
        _ui.update { it.copy(totalRon = total) }
    }

    /* ----- rate handling ----- */

    fun fetchRate() = viewModelScope.launch {
        val res = rateRepository.latestEurRon()
        res.onSuccess { rate ->
            _ui.update { it.copy(rateEurRon = rate, error = null) }
            recalcAllForNewRate()
        }.onFailure { e ->
            _ui.update { it.copy(error = e.message ?: "Rate fetch failed") }
        }
    }

    /** Manually set the EUR→RON rate from the text field. Recalculates all rows. */
    fun updateRate(rate: Double) {
        _ui.update { it.copy(rateEurRon = rate, error = null) }
        recalcAllForNewRate()
    }


    private fun recalcAllForNewRate() {
        _ui.update { s ->
            val r = s.rateEurRon
            val updated = s.rows.mapValues { (_, row) ->
                when (row.edited) {
                    Edited.EUR -> row.copy(ron = row.eur?.let { (it * r).round2() })
                    Edited.RON -> row.copy(eur = row.ron?.let { if (r > 0) (it / r).round2() else row.eur })
                    Edited.NONE -> row
                }
            }
            s.copy(rows = updated).recalcTotal()
        }
    }

    /* ----- helpers ----- */

    private fun RecurringUiState.recalcTotal(): RecurringUiState =
        copy(totalRon = rows.values.sumOf { it.ron ?: 0.0 })

    private fun Double.round2(): Double =
        kotlin.math.round(this * 100.0) / 100.0

    private fun defaultRows(): Map<String, RecurringRowUi> = listOf(
        // prefill Rent with 350 EUR so RON auto-computes when rate is fetched/changed
        "RENT" to RecurringRowUi("RENT", eur = 350.0, ron = null, edited = Edited.EUR),
        "GAZ" to RecurringRowUi("GAZ", eur = null, ron = null),
        "CURENT" to RecurringRowUi("CURENT", eur = null, ron = null),
        "DIGI" to RecurringRowUi("DIGI", eur = null, ron = null),
        "INTRETINERE" to RecurringRowUi("INTRETINERE", eur = null, ron = null)
    ).toMap()
}

/* -------------- factory -------------- */

class RecurringVMFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecurringViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecurringViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
