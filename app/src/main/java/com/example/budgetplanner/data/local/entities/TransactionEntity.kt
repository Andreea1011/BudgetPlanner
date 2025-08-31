package com.example.budgetplanner.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long,                 // epoch millis (booking date at local midnight)
    val originalAmount: Double,          // signed: expense = negative, income = positive
    val originalCurrency: String,        // "RON","EUR","USD"
    val merchant: String?,               // counterparty or description
    val note: String? = null,
    val category: String = "OTHER",      // simple string enum
    val source: String = "MANUAL",       // MANUAL | OPEN_BANKING | NOTIF | SMS
    val amountRon: Double = originalAmount, // (convert later with BNR)
    val pending: Boolean = false)