package com.example.budgetplanner.data.local.entities.dao


import androidx.room.*
import com.example.budgetplanner.data.local.entities.SavingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsDao {
    @Query("SELECT * FROM savings ORDER BY name")
    fun observeAll(): Flow<List<SavingsEntity>>

    @Query("SELECT SUM(amountRon) FROM savings")
    fun observeTotalRon(): Flow<Double?>
    @Query("SELECT * FROM savings WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): SavingsEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(e: SavingsEntity): Long

    @Update
    suspend fun update(e: SavingsEntity)

    @Query("UPDATE savings SET amountRon = :amount WHERE id = :id")
    suspend fun updateAmount(id: Long, amount: Double)

    @Query("DELETE FROM savings WHERE id = :id")
    suspend fun deleteById(id: Long)


}