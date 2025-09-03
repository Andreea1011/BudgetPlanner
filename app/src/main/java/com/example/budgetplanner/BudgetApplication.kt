package com.example.budgetplanner

import android.app.Application
import androidx.room.Room
import com.example.budgetplanner.data.local.BudgetDatabase
import com.example.budgetplanner.data.remote.RetrofitModule
import com.example.budgetplanner.data.repository.BudgetRepository
import com.example.budgetplanner.data.repository.RateRepository
import com.example.budgetplanner.data.repository.RecurringRepository
import com.example.budgetplanner.data.settings.RateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BudgetApplication : Application() {
    lateinit var repository: BudgetRepository
    lateinit var recurringRepository: RecurringRepository   // ✅ add
    lateinit var rateRepository: RateRepository

    override fun onCreate() {
        super.onCreate()
        val db = BudgetDatabase.getInstance(this)

        repository = BudgetRepository(
            transactionDao = db.transactionDao(),
            reimbursementLinkDao = db.reimbursementLinkDao(),
            savingsDao = db.savingsDao(),
            merchantRuleDao = db.merchantRuleDao()
        )

        recurringRepository = RecurringRepository(db.recurringExpenseDao())

        rateRepository = RateRepository(store = RateStore(this))

        CoroutineScope(Dispatchers.IO).launch {
            repository.seedDefaultRulesIfEmpty()
        }
    }

}