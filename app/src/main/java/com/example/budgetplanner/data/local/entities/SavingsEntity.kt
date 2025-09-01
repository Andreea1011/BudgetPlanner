package com.example.budgetplanner.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings")
data class SavingsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,           // e.g., "Mom surplus"
    val amountRon: Double = 0.0,
    val note: String? = null
)