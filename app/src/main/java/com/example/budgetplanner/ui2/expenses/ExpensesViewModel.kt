package com.example.budgetplanner.ui2.expenses

import android.app.Application
import androidx.lifecycle.*
import com.example.budgetplanner.BudgetApplication
import com.example.budgetplanner.domain.model.Category
import com.example.budgetplanner.domain.model.Transaction
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId

data class CategoryRow(val category: Category, val ron: Double)

/** UI model now also exposes:
 *  - selectedCategory to filter the list
 *  - items: the actual transactions visible for the selection
 *  - isLoading for simple progress toggling in the screen
 */
data class ExpensesUi(
    val month: YearMonth = YearMonth.now(),
    val rows: List<CategoryRow> = emptyList(),
    val total: Double = 0.0,
    val selectedCategory: Category? = null,
    val items: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
)

class ExpensesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as BudgetApplication).repository
    private val zone = ZoneId.systemDefault()

    private val _ui = MutableStateFlow(ExpensesUi())
    val ui: StateFlow<ExpensesUi> = _ui.asStateFlow()

    // keep all month transactions cached so we can filter quickly by category
    private var monthAll: List<Transaction> = emptyList()

    init { refresh(_ui.value.month) }

    fun prev() = refresh(_ui.value.month.minusMonths(1))
    fun next() = refresh(_ui.value.month.plusMonths(1))

    /** User taps a category chip (or “All” => null) */
    fun selectCategory(cat: Category?) {
        _ui.update { it.copy(selectedCategory = cat, items = filter(cat)) }
    }

    /** One-tap auto-relabel based on rules, then recompute totals/list */
    fun recategorize() = viewModelScope.launch {
        _ui.update { it.copy(isLoading = true) }
        runCatching { repo.recategorizeMonth(_ui.value.month, zone) } // no-op if you haven’t added rules yet
        refresh(_ui.value.month)
    }

    private fun refresh(m: YearMonth) = viewModelScope.launch {
        _ui.update { it.copy(isLoading = true, month = m) }

        // 1) totals by category (personal only)
        val map = repo.personalSpendByCategory(m, zone) // Map<Category, Double>
        val rows = map.entries
            .sortedByDescending { it.value }
            .map { CategoryRow(it.key, it.value) }
        val total = rows.sumOf { it.ron }

        // 2) cache all month transactions once; filter below
        monthAll = repo.observeMonth(m, zone).first()
            .filter { it.amountRon < 0 && !it.excludePersonal && it.party != "MOM" }

        _ui.update {
            it.copy(
                rows = rows,
                total = total,
                items = filter(it.selectedCategory),
                isLoading = false
            )
        }
    }

    private fun filter(cat: Category?): List<Transaction> =
        if (cat == null) monthAll
        else monthAll.filter { it.category == cat }
}

class ExpensesVMFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpensesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return ExpensesViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown VM class")
    }
}
