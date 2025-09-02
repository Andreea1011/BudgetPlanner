package com.example.budgetplanner.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * We store everything normalized in RON, but keep the last-edited EUR and rate
 * so the user sees both columns roundtrip.
 */
@Entity(
    tableName = "recurring_expenses",
    indices = [Index(value = ["monthStart", "name"], unique = true)]
)
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthStart: Long,
    val name: String,
    val amountEur: Double?,
    val amountRon: Double?,
    val rateEurRon: Double?
)