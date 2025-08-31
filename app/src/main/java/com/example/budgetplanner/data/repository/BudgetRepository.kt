package com.example.budgetplanner.data.repository


import com.example.budgetplanner.data.local.entities.dao.TransactionDao
import com.example.budgetplanner.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.*

class BudgetRepository(
    private val transactionDao: TransactionDao
) {
    fun observeMonth(yearMonth: YearMonth, zoneId: ZoneId = ZoneId.systemDefault()): Flow<List<Transaction>> {
        val start = yearMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return transactionDao.observeBetween(start, end).map { it.map { e -> e.toDomain() } }
    }

    // Stub for now — later this will call your backend OpenBanking endpoint
    suspend fun syncMonthFromBank(@Suppress("UNUSED_PARAMETER") yearMonth: YearMonth): Result<Unit> {
        return Result.success(Unit)
    }

    suspend fun addTransaction(tx: com.example.budgetplanner.domain.model.Transaction) {
        transactionDao.insert(tx.toEntity())
    }

    suspend fun deleteTransactionById(id: Long) =
        transactionDao.deleteById(id)

    suspend fun updateTransactionNoteAndCategory(id: Long, note: String?, category: String) =
        transactionDao.updateNoteAndCategory(id, note, category)

}