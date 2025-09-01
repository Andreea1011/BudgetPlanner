package com.example.budgetplanner.domain.model

data class Transaction(
    val id: Long = 0L,
    val timestamp: Long,
    val originalAmount: Double,
    val originalCurrency: String,
    val merchant: String?,
    val note: String?,
    val category: Category,
    val source: Source,
    val amountRon: Double,
    val pending: Boolean,
    val excludePersonal: Boolean = false,
    val party: String? = null
)


enum class Category { FOOD, TRANSPORT, DORINTE, APARTMENT, OTHER }
enum class Source { MANUAL, OPEN_BANKING, NOTIF, SMS }