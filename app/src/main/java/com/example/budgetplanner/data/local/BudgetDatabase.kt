package com.example.budgetplanner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.budgetplanner.data.local.entities.TransactionEntity
import com.example.budgetplanner.data.local.entities.dao.TransactionDao

@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BudgetDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}