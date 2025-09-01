package com.example.budgetplanner.data.repository

import com.example.budgetplanner.data.local.entities.RecurringExpenseEntity
import com.example.budgetplanner.data.local.entities.dao.RecurringExpenseDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.YearMonth
import java.time.ZoneId

class RecurringRepository(
    private val dao: RecurringExpenseDao
) {
    private fun monthStartMillis(m: YearMonth, zone: ZoneId) =
        m.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

    fun observeMonth(m: YearMonth, zone: ZoneId): Flow<List<RecurringExpenseEntity>> = flow {
        val items = dao.getForMonth(monthStartMillis(m, zone))
        emit(items)
    }

    suspend fun save(
        m: YearMonth,
        zone: ZoneId,
        name: String,
        eur: Double?,
        ron: Double?,
        rate: Double
    ) {
        val start = monthStartMillis(m, zone)
        val normalizedRon = when {
            eur != null -> eur * rate
            ron != null -> ron
            else -> 0.0
        }
        val inputCur = if (eur != null) "EUR" else "RON"
        val entity = RecurringExpenseEntity(
            monthStartMillis = start,
            name = name,
            amountRon = normalizedRon,
            amountEur = eur,
            rateEurRon = if (eur != null) rate else null,
            inputCurrency = inputCur
        )
        dao.upsert(entity)
    }

    suspend fun totalRon(m: YearMonth, zone: ZoneId): Double =
        dao.sumRonForMonth(monthStartMillis(m, zone))
}
