package com.example.budgetplanner.ui2.home

import android.app.Application
import androidx.lifecycle.*
import com.example.budgetplanner.BudgetApplication
import com.example.budgetplanner.domain.model.Category
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

data class CategorySlice(val label: String, val value: Double)
data class MonthPoint(val label: String, val value: Double)

data class HomeChartsUi(
    val month: YearMonth = YearMonth.now(),
    val categories: List<CategorySlice> = emptyList(),
    val monthlyTrend: List<MonthPoint> = emptyList()
)

class HomeChartsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as BudgetApplication).repository
    private val zone = ZoneId.systemDefault()

    private val _ui = MutableStateFlow(HomeChartsUi())
    val ui: StateFlow<HomeChartsUi> = _ui.asStateFlow()

    init { refresh(_ui.value.month) }

    fun prevMonth() = refresh(_ui.value.month.minusMonths(1))
    fun nextMonth() = refresh(_ui.value.month.plusMonths(1))

    fun refresh(month: YearMonth) = viewModelScope.launch {
        // 1) by-category for current month
        val map = repo.personalSpendByCategory(month, zone) // Map<Category, Double>
        val categories = map.entries
            .sortedByDescending { it.value }
            .map { (cat, ron) -> CategorySlice(label = cat.name, value = ron) }

        // 2) last 6 months trend (including current)
        val trendMonths = (5 downTo 0).map { month.minusMonths(it.toLong()) }
        val monthlyTrend = trendMonths.map { m ->
            val total = repo.personalSpendForMonth(m, zone)
            val label = m.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            MonthPoint(label = label, value = total)
        }

        _ui.value = HomeChartsUi(month = month, categories = categories, monthlyTrend = monthlyTrend)
    }
}

class HomeChartsVMFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeChartsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeChartsViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown VM class")
    }
}
