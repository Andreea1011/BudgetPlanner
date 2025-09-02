package com.example.budgetplanner.ui2.expenses

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as Application
    val vm: ExpensesViewModel = viewModel(factory = ExpensesVMFactory(app))
    val ui by vm.ui.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("All expenses") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            MonthSelector(ui.month, onPrev = vm::prev, onNext = vm::next)
            Spacer(Modifier.height(12.dp))

            Text(
                "Total personal spend: ${"%.2f".format(ui.total)} RON",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            if (ui.rows.isEmpty()) {
                Text("No expenses this month.")
            } else {
                HeaderRow()
                Divider()
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ui.rows) { r ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(r.category.name)
                            Text("${"%.2f".format(r.ron)} RON")
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSelector(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        OutlinedButton(onClick = onPrev) { Text("◀") }
        val label = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
        Text(label, style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = onNext) { Text("▶") }
    }
}

@Composable
private fun HeaderRow() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Category", fontWeight = FontWeight.SemiBold)
        Text("Amount (RON)", fontWeight = FontWeight.SemiBold)
    }
}
