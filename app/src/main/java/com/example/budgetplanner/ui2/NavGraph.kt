package com.example.budgetplanner.ui2

import androidx.compose.foundation.layout.padding   // <— for Modifier.padding
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AccountBalance

import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.budgetplanner.ui2.home.HomeScreen
import com.example.budgetplanner.ui2.recurring.RecurringScreen

/**
 * Call BudgetApp() from your MainActivity's setContent { ... }.
 */
@Composable
fun BudgetApp() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomBar(navController) }
    ) { innerPadding ->
        BudgetNavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/** Destinations used in the bottom bar */
sealed class Dest(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Home : Dest("home", "Home", Icons.Filled.Home)
    data object Transactions : Dest("transactions", "Transactions", Icons.Filled.ReceiptLong)
    data object Rent : Dest("rent", "Recurring", Icons.Filled.Assessment)
    data object Savings : Dest("savings", "Savings", Icons.Filled.AccountBalance)

    companion object {
        val bottom = listOf(Home, Transactions, Rent, Savings)
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    NavigationBar {
        Dest.Companion.bottom.forEach { dest ->
            val selected = currentRoute == dest.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(dest.route) {
                        // Avoid building a huge back stack when re-tapping items
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) }
            )
        }
    }
}

@Composable
fun BudgetNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Dest.Home.route,
        modifier = modifier
    ) {
        composable(Dest.Home.route) {
            HomeScreen(
                onViewSavings = { navController.navigate(Dest.Savings.route) },
                onViewTransactions = { navController.navigate(Dest.Transactions.route) },
                onViewRecurring = { navController.navigate(Dest.Rent.route) },
                onViewExpenses = { /* TODO: Add Expenses Screen route */ }
            )
        }


        composable("transactions") {
            com.example.budgetplanner.ui2.transactions.TransactionsScreen()
        }

        composable(Dest.Rent.route) {
            // Recurrent expenses screen
            RecurringScreen(onBack = { navController.popBackStack() })
        }

        composable("expenses") {
            com.example.budgetplanner.ui2.expenses.ExpensesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Dest.Savings.route) {
            com.example.budgetplanner.ui2.savings.SavingsScreen(
                onBack = { navController.popBackStack() }
            )
        }


    }
}






