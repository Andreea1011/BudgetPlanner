package com.example.budgetplanner.data.local.entities.dao

import androidx.room.*
import com.example.budgetplanner.data.local.entities.RecurringExpenseEntity

// app/data/local/entities/dao/RecurringExpenseDao.kt
import androidx.room.*

@Dao
interface RecurringExpenseDao {

    @Upsert
    suspend fun upsert(entity: RecurringExpenseEntity)

    @Upsert
    suspend fun upsertAll(list: List<RecurringExpenseEntity>)

    @Query("SELECT * FROM recurring_expenses WHERE monthStart = :monthStart")
    fun getForMonth(monthStart: Long): kotlinx.coroutines.flow.Flow<List<RecurringExpenseEntity>>

    @Query("SELECT SUM(COALESCE(amountRon,0)) FROM recurring_expenses WHERE monthStart = :monthStart")
    fun sumRonForMonth(monthStart: Long): kotlinx.coroutines.flow.Flow<Double?>

    // one-time cleanup helper
    @Query("""
        DELETE FROM recurring_expenses
        WHERE rowid NOT IN (
            SELECT MIN(rowid) FROM recurring_expenses
            GROUP BY monthStart, name
        )
    """)
    suspend fun dedupe()
}
