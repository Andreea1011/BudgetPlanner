package com.example.budgetplanner.data.local.entities.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.budgetplanner.data.local.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // Month filter: [start, end)
    @Query("""
        SELECT * FROM transactions
        WHERE timestamp >= :startMillis AND timestamp < :endMillis
        ORDER BY timestamp DESC, id DESC
    """)
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(e: TransactionEntity): Long

    @Update
    suspend fun update(e: TransactionEntity)
    @Delete
    suspend fun delete(e: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE transactions SET note = :note, category = :category WHERE id = :id")
    suspend fun updateNoteAndCategory(id: Long, note: String?, category: String)

    @Query("UPDATE transactions SET excludePersonal = :exclude WHERE id = :id")
    suspend fun setExcludePersonal(id: Long, exclude: Boolean)

    @Query("UPDATE transactions SET party = :party WHERE id = :id")
    suspend fun setParty(id: Long, party: String?)

    // read single tx
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    // candidates: “mom expenses” within a window, newest first
    @Query("""
    SELECT * FROM transactions
    WHERE pending = 0
      AND excludePersonal = 1
      AND originalAmount < 0
      AND timestamp BETWEEN :fromMillis AND :toMillis
    ORDER BY timestamp DESC, id DESC
""")
    suspend fun getMomExpenseCandidates(fromMillis: Long, toMillis: Long): List<TransactionEntity>

    @Query("""
    SELECT * FROM transactions
    WHERE pending = 0
      AND originalAmount > 0
      AND party = 'MOM'
      AND timestamp BETWEEN :fromMillis AND :toMillis
    ORDER BY timestamp DESC, id DESC
""")
    suspend fun getMomCredits(fromMillis: Long, toMillis: Long): List<TransactionEntity>

    @Query("""
    SELECT COALESCE(SUM(ABS(amountRon)), 0) FROM transactions
    WHERE pending = 0 AND originalAmount < 0 AND excludePersonal = 0
      AND timestamp BETWEEN :fromMillis AND :toMillis
""")
    suspend fun sumPersonalSpendBetween(fromMillis: Long, toMillis: Long): Double

    @Query("""
    SELECT * FROM transactions
    WHERE pending = 0 AND originalAmount < 0 AND excludePersonal = 1
      AND timestamp BETWEEN :fromMillis AND :toMillis
""")
    suspend fun getExcludedExpensesBetween(fromMillis: Long, toMillis: Long): List<TransactionEntity>

    @Query("""
    SELECT * FROM transactions
    WHERE timestamp >= :start AND timestamp < :end
    ORDER BY timestamp DESC
""")
    suspend fun getBetween(start: Long, end: Long): List<TransactionEntity>

}