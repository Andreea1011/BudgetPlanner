// app/src/main/java/com/example/budgetplanner/data/local/BudgetDatabase.kt
package com.example.budgetplanner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.budgetplanner.data.local.entities.*
import com.example.budgetplanner.data.local.entities.dao.*

@Database(
    entities = [
        TransactionEntity::class,
        SavingsEntity::class,
        ReimbursementLinkEntity::class,
        MerchantRuleEntity::class,          // <-- rules entity
        RecurringExpenseEntity::class       // if you use recurring table
    ],
    version = 2,                            // bump when adding MerchantRuleEntity
    exportSchema = false
)
abstract class BudgetDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun savingsDao(): SavingsDao
    abstract fun reimbursementLinkDao(): ReimbursementLinkDao
    abstract fun merchantRuleDao(): com.example.budgetplanner.data.local.entities.dao.MerchantRuleDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao

    companion object {
        @Volatile private var INSTANCE: BudgetDatabase? = null

        fun getInstance(context: Context): BudgetDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BudgetDatabase::class.java,
                    "budget.db"
                )
                    .fallbackToDestructiveMigration() // keep while developing
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
