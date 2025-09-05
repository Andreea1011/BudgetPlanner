package com.example.budgetplanner.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.budgetplanner.domain.model.Transaction

@Entity(
    tableName = "transactions",
    indices = [
        Index(
            value = ["timestamp", "originalAmount", "originalCurrency", "merchantNorm"],
            unique = true
        )
    ]
)
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
    val pending: Boolean = false,
    val excludePersonal: Boolean = false,   // manually mark “for mom” etc.
    val party: String? = null,              // "MOM" on reimbursements (credits)
    val reimbursedGroup: String? = null,     // "MOM" when fully covered (nice to display)// optional soft-link label like "MOM"
    @ColumnInfo(defaultValue = "") val merchantNorm: String = merchant?.trim()?.uppercase() ?: ""
    )

private fun Transaction.toEntity(): TransactionEntity =
    TransactionEntity(
        id = id,
        timestamp = timestamp,
        originalAmount = originalAmount,
        originalCurrency = originalCurrency,
        merchant = merchant,
        merchantNorm = merchant?.trim()?.uppercase() ?: "",
    )
