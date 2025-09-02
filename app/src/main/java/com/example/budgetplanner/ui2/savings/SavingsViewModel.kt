package com.example.budgetplanner.ui2.savings

import android.app.Application
import androidx.lifecycle.*
import com.example.budgetplanner.BudgetApplication
import com.example.budgetplanner.data.local.entities.SavingsEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update



data class SavingsRowUi(
    val id: Long,
    val name: String,
    val amountRon: Double,
    val amountEur: Double?   // computed using EUR→RON
)

data class SavingsUiState(
    val rows: List<SavingsRowUi> = emptyList(),
    val totalRon: Double = 0.0,
    val totalEur: Double? = null,
    val rateEurRon: Double = 0.0,
    val error: String? = null,
    val showAddDialog: Boolean = false
)

class SavingsViewModel(app: Application) : AndroidViewModel(app) {

    // Grab your App subclass (this is the key fix)
    private val appCtx: BudgetApplication = getApplication()

    // Repositories from the app
    private val repository = appCtx.repository
    private val rateRepository = appCtx.rateRepository  // <- now resolves

    private val _ui = MutableStateFlow(SavingsUiState())
    val ui: StateFlow<SavingsUiState> = _ui.asStateFlow()

    private companion object {
        const val MOM_POT = "Mom surplus"
    }

    init {
        viewModelScope.launch { repository.ensureSavingsPot(MOM_POT) }
        observeAll()
    }

    private fun observeAll() {
        val savingsFlow = repository.observeSavings()            // Flow<List<SavingsEntity>>
        val totalRonFlow = repository.observeSavingsTotalRon()   // Flow<Double?>

        // If your RateRepository exposes a flow:
        val rateFlow: Flow<Double> = rateRepository.rateFlow()
            .map { r -> if (r > 0.0) r else 0.0 }
            .catch { emit(0.0) }

        viewModelScope.launch {
            combine(savingsFlow, totalRonFlow, rateFlow) { list, totalRon, rate ->
                val rows = list.map { e ->
                    SavingsRowUi(
                        id = e.id,
                        name = e.name,
                        amountRon = e.amountRon,
                        amountEur = if (rate > 0.0) e.amountRon / rate else null
                    )
                }
                val totalEur = if (rate > 0.0) (totalRon ?: 0.0) / rate else null
                SavingsUiState(
                    rows = rows,
                    totalRon = totalRon ?: 0.0,
                    totalEur = totalEur,
                    rateEurRon = rate
                )
            }.collect { _ui.value = it }
        }
    }

    fun openAddDialog()  { _ui.update { it.copy(showAddDialog = true) } }
    fun closeAddDialog() { _ui.update { it.copy(showAddDialog = false) } }

    fun addPot(name: String, amountRon: Double) = viewModelScope.launch {
        repository.addSavings(name.trim(), amountRon)
        closeAddDialog()
    }

    fun updateAmount(id: Long, amountRon: Double) = viewModelScope.launch {
        repository.setSavingsAmount(id, amountRon)
    }

    fun delete(id: Long) = viewModelScope.launch {
        repository.deleteSavings(id)
    }
}

class SavingsVMFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return SavingsViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown VM class")
    }
}
