package com.example.budgetplanner.ui2.transactions

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetplanner.ui2.components.TransactionBottomSheet
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen() {
    val app = LocalContext.current.applicationContext as Application
    val vm: TransactionsViewModel = viewModel(factory = TransactionsVMFactory(app))

    val state by vm.ui.collectAsState()
    val selected = state.selected

    // Single bottom sheet (details + toggles + actions)
    if (selected != null) {
        TransactionBottomSheet(
            tx = selected,
            onDismiss = { vm.dismissSheet() },
            onSaveNoteAndCategory = { note, category -> vm.saveSelected(note, category) },
            onToggleExclude = { checked -> vm.setExcludePersonal(selected.id, checked) },
            onToggleMom = { checked -> vm.setParty(selected.id, if (checked) "MOM" else null) },
            onDelete = { vm.deleteSelected() },
            // show live info in the sheet:
            expenseCoverage = state.expenseCoverage,
            creditAllocation = state.creditAllocation
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Transactions") },
                actions = {
                    TextButton(onClick = { vm.seedDemo() }) { Text("Seed") }
                    TextButton(onClick = { vm.sync() }, enabled = !state.isLoading) {
                        Text(if (state.isLoading) "Syncing…" else "Sync")
                    }
                    TextButton(onClick = { vm.seedMomScenario() }) { Text("Seed Mom") }
                    TextButton(onClick = { vm.autoLabel() }) { Text("Auto-label") }
                    TextButton(onClick = { vm.dumpMomLinksToLog() }) { Text("Dump") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {

            // month selector
            MonthSelector(
                month = state.month,
                onPrev = { vm.prevMonth() },
                onNext = { vm.nextMonth() }
            )

            // month totals line
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Personal spend: ${
                    "%.2f".format(state.totalPersonalSpend)
                } RON   •   Open for Mom: ${
                    "%.2f".format(state.totalOpenForMom)
                } RON",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(12.dp))

            // table
            TransactionsTable(
                ui = state,
                onRowClick = { vm.onRowClick(it.tx) },
                amountFormatter = { amt, cur -> vm.formatSigned(amt, cur) }
            )

            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text("Error: $it", color = MaterialTheme.colorScheme.error)
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
private fun TransactionsTable(
    ui: TransactionsUiState,
    onRowClick: (TxRowUi) -> Unit,
    amountFormatter: (Double, String) -> String
) {
    if (ui.rows.isEmpty()) {
        Text("No transactions this month.")
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ui.rows.forEach { group ->
            stickyHeader {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        group.date.format(
                            DateTimeFormatter.ofPattern("dd MMMM, EEEE", Locale.getDefault())
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
            items(group.items) { row ->
                val t = row.tx
                val amountText = amountFormatter(t.originalAmount, t.originalCurrency)
                val color = if (t.originalAmount < 0)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onRowClick(row) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(t.merchant ?: (t.note ?: "—"), Modifier.weight(1f))
                    Text(amountText, color = color)
                }
                Divider()
            }
        }
    }
}
