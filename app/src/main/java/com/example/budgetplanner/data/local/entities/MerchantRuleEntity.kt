package com.example.budgetplanner.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_rules")
data class MerchantRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchType: String = "CONTAINS", // CONTAINS | STARTS_WITH | EXACT
    val pattern: String,                // merchant text pattern
    val category: String? = null,       // store enum name as STRING (e.g., "FOOD")
    val excludePersonal: Boolean = false,
    val setParty: String? = null,       // e.g., "MOM"
    val priority: Int = 100
)
