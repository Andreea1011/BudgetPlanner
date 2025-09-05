package com.example.budgetplanner.data.repository


import com.example.budgetplanner.data.local.entities.TransactionEntity
import com.example.budgetplanner.domain.model.Category
import com.example.budgetplanner.domain.model.Source
import com.example.budgetplanner.domain.model.Transaction

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    timestamp = timestamp,
    originalAmount = originalAmount,
    originalCurrency = originalCurrency,
    merchant = merchant,
    note = note,
    category = Category.valueOf(category),
    source = Source.valueOf(source),
    amountRon = amountRon,
    pending = pending,
    excludePersonal = excludePersonal,
    party = party
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    timestamp = timestamp,
    originalAmount = originalAmount,
    originalCurrency = originalCurrency,
    merchant = merchant,
    merchantNorm = merchant?.trim()?.uppercase() ?: "",
    note = note,
    category = category.name,
    source = source.name,
    amountRon = amountRon,
    pending = pending,
    excludePersonal = excludePersonal,
    party = party
)