package com.example.budgetplanner.ui2.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.lazy.stickyHeader
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.budgetplanner.domain.model.Category
import com.example.budgetplanner.BuildConfig
import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.foundation.clickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen() {
    val app = LocalContext.current.applicationContext as android.app.Application
    val vm: TransactionsViewModel = viewModel(factory = TransactionsVMFactory(app))
    val ui by vm.ui.collectAsState()


    var showSheet by remember { mutableStateOf(false) }
    LaunchedEffect(ui.selected) { showSheet = ui.selected != null }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Transactions") },
                actions = {
                    if (BuildConfig.DEBUG) {
                        TextButton(onClick = { vm.seedDemo() }) { Text("Seed") }
                    }
                    TextButton(onClick = { vm.sync() }, enabled = !ui.isLoading) {
                        Text(if (ui.isLoading) "Syncing…" else "Sync")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            MonthSelector(
                month = ui.month,
                onPrev = { vm.prevMonth() },
                onNext = { vm.nextMonth() }
            )
            Spacer(Modifier.height(8.dp))
            TransactionsTable(
                ui = ui,
                onRowClick = { vm.onRowClick(it.tx) },
                amountFormatter = { amt, cur -> vm.formatSigned(amt, cur) }
            )
            ui.error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { vm.dismissSheet() }) {
            val t = ui.selected!!
            var note by remember(t.id) { mutableStateOf(t.note ?: "") }
            var category by remember(t.id) { mutableStateOf(t.category) }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Details", style = MaterialTheme.typography.titleMedium)
                Text("Merchant: ${t.merchant ?: "—"}")
                Text("Date: " + java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(java.util.Date(t.timestamp)))
                Text("Amount: " + vm.formatSigned(t.originalAmount, t.originalCurrency))
                // Category picker
                // ---- Simple Category dropdown (no Exposed APIs) ----
                var catExpanded by remember { mutableStateOf(false) }

                Box {
                    OutlinedTextField(
                        value = category.name,
                        onValueChange = {},
                        label = { Text("Category") },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { catExpanded = true }
                    )
                    DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        com.example.budgetplanner.domain.model.Category.entries.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = { category = c; catExpanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") })
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { vm.saveSelected(note.ifBlank { null }, category) }) { Text("Save") }
                    OutlinedButton(onClick = { vm.deleteSelected() }) { Text("Delete") }
                    TextButton(onClick = { vm.dismissSheet() }) { Text("Close") }
                }
                Spacer(Modifier.height(12.dp))
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
                        group.date.format(DateTimeFormatter.ofPattern("dd MMMM, EEEE", Locale.getDefault())),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
            items(group.items) { row ->
                val t = row.tx
                val amountText = amountFormatter(t.originalAmount, t.originalCurrency)
                val color = if (t.originalAmount < 0) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
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