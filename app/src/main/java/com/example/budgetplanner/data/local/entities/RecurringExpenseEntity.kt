package com.example.budgetplanner.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * We store everything normalized in RON, but keep the last-edited EUR and rate
 * so the user sees both columns roundtrip.
 */
@Entity(tableName = "recurring_expenses")
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val monthStartMillis: Long,           // first day of month @ 00:00
    val name: String,                     // RENT / GAZ / CURENT / DIGI / INTRETINERE
    val amountRon: Double,                // authoritative value used for totals
    val amountEur: Double?,               // nullable if entered in RON
    val rateEurRon: Double?,              // the rate used when EUR was entered
    val inputCurrency: String             // "RON" or "EUR" (the last edited column)
)
