package com.example.budgetplanner.data.repository


import com.example.budgetplanner.data.local.entities.TransactionEntity
import com.example.budgetplanner.domain.model.*

fun TransactionEntity.toDomain() = Transaction(
    id, timestamp, originalAmount, originalCurrency, merchant, note,
    Category.valueOf(category), Source.valueOf(source), amountRon, pending
)

fun Transaction.toEntity() = TransactionEntity(
    id, timestamp, originalAmount, originalCurrency, merchant, note,
    category.name, source.name, amountRon, pending
)