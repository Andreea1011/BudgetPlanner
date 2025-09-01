package com.example.budgetplanner.data.local.entities.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.budgetplanner.data.local.entities.ReimbursementLinkEntity

@Dao
interface ReimbursementLinkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(link: ReimbursementLinkEntity): Long

    @Query("SELECT COALESCE(SUM(amountRon), 0) FROM reimbursement_links WHERE expenseTxId = :expenseId")
    suspend fun sumCoveredForExpense(expenseId: Long): Double

    @Query("SELECT COALESCE(SUM(amountRon), 0) FROM reimbursement_links WHERE creditTxId = :creditId")
    suspend fun sumAllocatedFromCredit(creditId: Long): Double

    @Query("SELECT * FROM reimbursement_links WHERE creditTxId = :creditId ORDER BY id DESC")
    suspend fun getLinksForCredit(creditId: Long): List<ReimbursementLinkEntity>

    @Query("SELECT * FROM reimbursement_links WHERE expenseTxId = :expenseId ORDER BY id DESC")
    suspend fun getLinksForExpense(expenseId: Long): List<ReimbursementLinkEntity>

    @Query("DELETE FROM reimbursement_links WHERE id = :id")
    suspend fun deleteById(id: Long)
}