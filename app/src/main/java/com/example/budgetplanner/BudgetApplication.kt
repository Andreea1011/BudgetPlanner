package com.example.budgetplanner

import android.app.Application
import androidx.room.Room
import com.example.budgetplanner.data.local.BudgetDatabase
import com.example.budgetplanner.data.repository.BudgetRepository

class BudgetApplication : Application() {
    val database: BudgetDatabase by lazy {
        Room.databaseBuilder(this, BudgetDatabase::class.java, "budget.db")
            .fallbackToDestructiveMigration() // OK while prototyping
            .build()
    }
    val repository: BudgetRepository by lazy {
        BudgetRepository(database.transactionDao())
    }
}