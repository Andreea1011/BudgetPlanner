package com.example.budgetplanner.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reimbursement_links",
    indices = [
        Index("expenseTxId"),
        Index("creditTxId")
    ]
)
data class ReimbursementLinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val expenseTxId: Long,   // FK -> transactions.id (expense; negative amount)
    val creditTxId: Long,    // FK -> transactions.id (credit from MOM; positive amount)
    val amountRon: Double,   // how much of that expense is covered by this credit (>= 0)
    val createdAt: Long = System.currentTimeMillis()
)