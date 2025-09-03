package com.example.budgetplanner.ui2.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.budgetplanner.BudgetApplication
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs

data class HomeUiState(
    val month: YearMonth = YearMonth.now(),
    val totalSavingsRon: Double = 0.0,
    val personalSpendRonThisMonth: Double = 0.0,
    val currentBalanceRon: Double? = null, // optional, we’ll wire initial balance later
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as BudgetApplication).repository
    private val zone: ZoneId = ZoneId.systemDefault()

    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    init { observe() }

    private fun observe() = viewModelScope.launch {
        combine(
            repo.observeSavingsTotalRon().map { it ?: 0.0 },
            repo.observePersonalSpendForMonth(_ui.value.month, zone),
            repo.observeNetTxSum() // placeholder for future “current balance”
        ) { totalSavings, personalSpendNeg, netTx ->
            // personalSpendNeg is negative; show absolute value for UI
            val personalSpendAbs = abs(personalSpendNeg)
            _ui.value.copy(
                totalSavingsRon = totalSavings,
                personalSpendRonThisMonth = personalSpendAbs,
                // currentBalanceRon = initialBalance + netTx   // we’ll add initialBalance soon
                currentBalanceRon = null,
                isLoading = false,
                error = null
            )
        }.catch { e ->
            _ui.update { it.copy(error = e.message, isLoading = false) }
        }.collect { newState ->
            _ui.value = newState
        }
    }

    fun prevMonth() { setMonth(_ui.value.month.minusMonths(1)) }
    fun nextMonth() { setMonth(_ui.value.month.plusMonths(1)) }

    private fun setMonth(m: YearMonth) {
        _ui.update { it.copy(month = m, isLoading = true) }
        // restart the personal-spend stream for the new month
        viewModelScope.launch {
            combine(
                repo.observeSavingsTotalRon().map { it ?: 0.0 },
                repo.observePersonalSpendForMonth(m, zone),
                repo.observeNetTxSum()
            ) { totalSavings, personalSpendNeg, netTx ->
                _ui.value.copy(
                    month = m,
                    totalSavingsRon = totalSavings,
                    personalSpendRonThisMonth = kotlin.math.abs(personalSpendNeg),
                    currentBalanceRon = null,
                    isLoading = false,
                    error = null
                )
            }.catch { e ->
                _ui.update { it.copy(error = e.message, isLoading = false) }
            }.collect { newState ->
                _ui.value = newState
            }
        }
    }
}

class HomeVMFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown VM class")
    }
}
