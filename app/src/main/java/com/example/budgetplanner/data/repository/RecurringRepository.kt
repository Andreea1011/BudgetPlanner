package com.example.budgetplanner.data.repository

import com.example.budgetplanner.data.local.entities.RecurringExpenseEntity
import com.example.budgetplanner.data.local.entities.dao.RecurringExpenseDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import java.time.ZoneId

class RecurringRepository(
    private val dao: RecurringExpenseDao
) {
    private fun monthStart(m: YearMonth, zone: ZoneId): Long =
        m.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

    /** Stream rows for a month straight from DAO. */
    fun observeMonth(m: YearMonth, zone: ZoneId): Flow<List<RecurringExpenseEntity>> {
        val start = monthStart(m, zone)
        return dao.getForMonth(start)   // DAO should return Flow<List<...>>
    }

    /** Upsert a single row (one name) for the selected month. */
    suspend fun save(
        m: YearMonth,
        zone: ZoneId,
        name: String,
        eur: Double?,
        ron: Double?,
        rate: Double
    ) {
        val start = monthStart(m, zone)

        // Normalize RON so totals are consistent
        val normalizedRon = when {
            eur != null -> eur * rate
            ron != null -> ron
            else -> 0.0
        }

        val entity = RecurringExpenseEntity(
            // use the actual entity property names
            monthStart = start,
            name = name,
            amountEur = eur,
            amountRon = normalizedRon,
            rateEurRon = if (eur != null) rate else null
        )

        dao.upsert(entity)  // or insert(onConflict = REPLACE) if your Room version lacks @Upsert
    }

    /** One-shot total RON from DB (Flow<Double?> → Double). */
    suspend fun totalRon(m: YearMonth, zone: ZoneId): Double {
        val start = monthStart(m, zone)
        return dao.sumRonForMonth(start).first() ?: 0.0
    }
}
