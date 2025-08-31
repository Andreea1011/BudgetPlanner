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

}