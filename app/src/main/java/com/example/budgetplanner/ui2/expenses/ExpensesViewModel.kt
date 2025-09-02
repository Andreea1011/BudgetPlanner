package com.example.budgetplanner.ui2.expenses

import android.app.Application
import androidx.lifecycle.*
import com.example.budgetplanner.BudgetApplication
import com.example.budgetplanner.domain.model.Category
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId

data class CategoryRow(val category: Category, val ron: Double)
data class ExpensesUi(
    val month: YearMonth = YearMonth.now(),
    val rows: List<CategoryRow> = emptyList(),
    val total: Double = 0.0,
)

class ExpensesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as BudgetApplication).repository
    private val zone = ZoneId.systemDefault()

    private val _ui = MutableStateFlow(ExpensesUi())
    val ui: StateFlow<ExpensesUi> = _ui.asStateFlow()

    init { refresh(_ui.value.month) }

    fun prev() = refresh(_ui.value.month.minusMonths(1))
    fun next() = refresh(_ui.value.month.plusMonths(1))

    private fun refresh(m: YearMonth) = viewModelScope.launch {
        val map = repo.personalSpendByCategory(m, zone)
        val rows = map.entries
            .sortedByDescending { it.value }
            .map { CategoryRow(it.key, it.value) }
        val total = rows.sumOf { it.ron }
        _ui.value = ExpensesUi(month = m, rows = rows, total = total)
    }
}

class ExpensesVMFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpensesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return ExpensesViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown VM class")
    }
}
