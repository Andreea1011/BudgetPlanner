package com.example.budgetplanner.data.local.entities.dao


import androidx.room.*
import com.example.budgetplanner.data.local.entities.SavingsEntity

@Dao
interface SavingsDao {
    @Query("SELECT * FROM savings WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): SavingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(e: SavingsEntity): Long

    @Query("UPDATE savings SET amountRon = :newAmount WHERE id = :id")
    suspend fun updateAmount(id: Long, newAmount: Double)
}