package com.example.budgetplanner.ui2.home

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetplanner.ui2.theme.WhiteButton
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    // charts VM (for the monthly trend)
    val chartsVm: HomeChartsViewModel = viewModel(factory = HomeChartsVMFactory(app))
    val charts by chartsVm.ui.collectAsState()

    // Keep charts in sync with the selected month
    LaunchedEffect(ui.month) { chartsVm.refresh(ui.month) }

    val monthLabel = remember(ui.month) {
        ui.month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Home",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(4.dp))

        // Month selector
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { vm.prevMonth() }) { Text("◀") }
            Text(monthLabel, style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { vm.nextMonth() }) { Text("▶") }
        }

        // KPI card
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Total savings", style = MaterialTheme.typography.labelLarge)
                Text(
                    "${"%.2f".format(ui.totalSavingsRon)} RON",
                    style = MaterialTheme.typography.headlineSmall
                )

                ui.currentBalanceRon?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("Current balance", style = MaterialTheme.typography.labelLarge)
                    Text("${"%.2f".format(it)} RON", style = MaterialTheme.typography.titleLarge)
                }

                Spacer(Modifier.height(8.dp))
                Text("Personal spend ($monthLabel)", style = MaterialTheme.typography.labelLarge)
                Text(
                    "${"%.2f".format(ui.personalSpendRonThisMonth)} RON",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Single chart: total monthly spendings (last N months)
        Text("Total monthly spendings", style = MaterialTheme.typography.titleMedium)
        MonthlyTrendBar(months = charts.monthlyTrend)

        Spacer(Modifier.height(16.dp))

        // Navigation buttons
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            WhiteButton(onClick = onViewSavings, modifier = Modifier.weight(1f),
            ) {
                Text("View all savings", color = Color.Black, textAlign = TextAlign.Center)
            }
            WhiteButton(onClick = onViewTransactions, modifier = Modifier.weight(1f)) {
                Text("View all transactions", color = Color.Black, textAlign = TextAlign.Center)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            WhiteButton(onClick = onViewRecurring, modifier = Modifier.weight(1f)) {
                Text("View recurrent transactions", color = Color.Black, textAlign = TextAlign.Center)
            }
            WhiteButton(onClick = onViewExpenses, modifier = Modifier.weight(1f)) {
                Text("View all expenses", color = Color.Black, textAlign = TextAlign.Center)
            }
        }

    }
}
