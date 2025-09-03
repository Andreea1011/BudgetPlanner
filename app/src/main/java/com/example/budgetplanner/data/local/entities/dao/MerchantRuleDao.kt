package com.example.budgetplanner.data.local.entities.dao

import androidx.room.*
import com.example.budgetplanner.data.local.entities.MerchantRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantRuleDao {
    @Query("SELECT * FROM merchant_rules ORDER BY priority ASC, id ASC")
    fun observeAll(): Flow<List<MerchantRuleEntity>>

    @Query("SELECT * FROM merchant_rules ORDER BY priority ASC, id ASC")
    suspend fun getAll(): List<MerchantRuleEntity>

    @Query("SELECT COUNT(*) FROM merchant_rules")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MerchantRuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MerchantRuleEntity)
    @Insert suspend fun insert(e: MerchantRuleEntity): Long
    @Update suspend fun update(e: MerchantRuleEntity)
    @Delete suspend fun delete(e: MerchantRuleEntity)
    @Query("DELETE FROM merchant_rules WHERE id=:id") suspend fun deleteById(id: Long)
}
