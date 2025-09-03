package com.example.budgetplanner.ui2.home

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.layout.Box // <-- needed for the placeholders

@Composable
fun HomeScreen(
    onViewSavings: () -> Unit,
    onViewTransactions: () -> Unit,
    onViewRecurring: () -> Unit,
    onViewExpenses: () -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val vm: HomeViewModel = viewModel(factory = HomeVMFactory(app))
    val ui by vm.ui.collectAsState()

    Column(

        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Month selector
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = { vm.prevMonth() }) { Text("◀") }
            val monthLabel = ui.month.format(
                DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault())
            ).replaceFirstChar { it.titlecase(Locale.getDefault()) }
            Text(monthLabel, style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { vm.nextMonth() }) { Text("▶") }
        }

        // KPI card
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Total savings", style = MaterialTheme.typography.labelLarge)
                Text("${"%.2f".format(ui.totalSavingsRon)} RON", style = MaterialTheme.typography.headlineSmall)
                val monthLabel = remember(ui.month) {
                    ui.month
                        .format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))
                        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
                }


                ui.currentBalanceRon?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("Current balance", style = MaterialTheme.typography.labelLarge)
                    Text("${"%.2f".format(it)} RON", style = MaterialTheme.typography.titleLarge)
                }

                Spacer(Modifier.height(8.dp))
                Text("Personal spend ($monthLabel)", style = MaterialTheme.typography.labelLarge)
                Text("${"%.2f".format(ui.personalSpendRonThisMonth)} RON", style = MaterialTheme.typography.titleLarge)
            }
        }

        // Navigation buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = onViewSavings, modifier = Modifier.weight(1f)) {
                Text("View all savings")
            }
            Button(onClick = onViewTransactions, modifier = Modifier.weight(1f)) {
                Text("View all transactions")
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = onViewRecurring, modifier = Modifier.weight(1f)) {
                Text("View recurrent transactions")
            }
            Button(onClick = onViewExpenses, modifier = Modifier.weight(1f)) {
                Text("View all expenses")
            }
        }

        // Placeholder charts
        Spacer(Modifier.height(16.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("Chart with spendings by category")
            }
        }

        Spacer(Modifier.height(16.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("Chart with total monthly spendings")
            }
        }
    }
}
