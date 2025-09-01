package com.example.budgetplanner.data.local.entities.dao

import androidx.room.*
import com.example.budgetplanner.data.local.entities.RecurringExpenseEntity

@Dao
interface RecurringExpenseDao {
    @Query("SELECT * FROM recurring_expenses WHERE monthStartMillis = :monthStart ORDER BY name")
    suspend fun getForMonth(monthStart: Long): List<RecurringExpenseEntity>

    @Query("SELECT COALESCE(SUM(amountRon), 0) FROM recurring_expenses WHERE monthStartMillis = :monthStart")
    suspend fun sumRonForMonth(monthStart: Long): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: RecurringExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<RecurringExpenseEntity>)
}
