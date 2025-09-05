package com.example.budgetplanner.ui2

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.budgetplanner.ui2.home.HomeScreen
import com.example.budgetplanner.ui2.recurring.RecurringScreen
import com.example.budgetplanner.ui2.transactions.TransactionsScreen

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
    data object Transactions : Dest("transactions", "Transactions",
        Icons.AutoMirrored.Filled.ReceiptLong
    )
    data object Rent : Dest("rent", "Recurring", Icons.Filled.Assessment)
    data object Savings : Dest("savings", "Savings", Icons.Filled.AccountBalance)

    data object Expenses : Dest("expenses", "Expenses", Icons.Filled.Assessment)     // <— NEW
    companion object { val bottom = listOf(Home, Transactions, Rent, Savings, Expenses) }
}


@Composable
private fun BottomBar(navController: NavHostController) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    NavigationBar {
        Dest.bottom.forEach { dest ->
            NavigationBarItem(
                selected = currentRoute == dest.route,
                onClick = {
                    navController.navigate(dest.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(dest.icon, dest.label) },
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
                onViewExpenses = { navController.navigate(Dest.Expenses.route) }
            )
        }


        composable(Dest.Transactions.route) {
            TransactionsScreen(
                onBack = {
                    // Try popping first; if there’s nothing to pop, go to home
                    if (!navController.popBackStack()) {
                        navController.navigate("home") {
                            launchSingleTop = true;
                            popUpTo("home") { inclusive = false }
                        }
                    }
                }
            )
        }

        composable(Dest.Rent.route) {
            // Recurrent expenses screen
            RecurringScreen(onBack = { navController.popBackStack() })
        }

        composable(Dest.Expenses.route) {
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






