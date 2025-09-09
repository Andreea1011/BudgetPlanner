package com.example.budgetplanner.ui2.transactions

import android.Manifest
import android.app.Application
import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetplanner.sms.SmsImporter
import com.example.budgetplanner.ui2.components.TransactionBottomSheet
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(onBack: (() -> Unit)? = null) {
    val app = LocalContext.current.applicationContext as Application
    val vm: TransactionsViewModel = viewModel(factory = TransactionsVMFactory(app))

    val state by vm.ui.collectAsState()
    val selected = state.selected
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    // --- Import SMS permission flow ---
    val readSmsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch {
            if (granted) {
                val imported = SmsImporter.importRecent(ctx, days = 14)
                snackbarHostState.showSnackbar("Imported $imported SMS transactions")
                runCatching { vm.autoLabel() }
            } else {
                snackbarHostState.showSnackbar("SMS permission denied")
            }
        }
    }

    // --- Photo Picker for receipts (works for pick or capture) ---
    val pickPhoto = rememberLauncherForActivityResult(PickVisualMedia()) { uri: Uri? ->
        uri?.let { vm.onImagePicked(it) }
    }

    if (selected != null) {
        TransactionBottomSheet(
            tx = selected,
            onDismiss = { vm.dismissSheet() },
            onSaveNoteAndCategory = { note, category -> vm.saveSelected(note, category) },
            onToggleExclude = { checked -> vm.setExcludePersonal(selected.id, checked) },
            onToggleMom = { checked -> vm.setParty(selected.id, if (checked) "MOM" else null) },
            onDelete = { vm.deleteSelected() },
            expenseCoverage = state.expenseCoverage,
            creditAllocation = state.creditAllocation
        )
    }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Transactions", style = MaterialTheme.typography.headlineLarge) },
                navigationIcon = {
                    IconButton(onClick = { onBack?.invoke() ?: backDispatcher?.onBackPressed() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(Modifier.padding(padding).padding(16.dp)) {

            // ACTION BUTTONS under the title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                ActionPillButton(text = "Auto-label") { vm.autoLabel() }
                Spacer(Modifier.width(12.dp))
                ActionPillButton(text = "Import SMS") {
                    readSmsLauncher.launch(Manifest.permission.READ_SMS)
                }
                Spacer(Modifier.width(12.dp))
                // NEW: Scan receipt
                ActionPillButton(text = "Scan receipt") {
                    pickPhoto.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                }
            }

            // Month selector
            MonthSelector(
                month = state.month,
                onPrev = { vm.prevMonth() },
                onNext = { vm.nextMonth() }
            )

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

    // === NEW: Receipt confirmation bottom sheet ===
    if (state.receipt is ReceiptUiState.Editing) {
        val s = state.receipt as ReceiptUiState.Editing
        ReceiptConfirmSheet(
            initial = s.draft,
            suggestions = s.vendorSuggestions,
            onDismiss = { vm.dismissReceipt() },
            onSave = { vm.saveReceipt(it) }
        )
    }
}

@Composable
private fun ActionPillButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onSurface,
            contentColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun MonthSelector(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiptConfirmSheet(
    initial: ReceiptDraft,
    suggestions: List<String>,
    onDismiss: () -> Unit,
    onSave: (ReceiptDraft) -> Unit
) {
    var vendor by remember { mutableStateOf(initial.vendor) }
    var totalText by remember { mutableStateOf(initial.totalRon.toPlainString()) }
    var dateText by remember {
        mutableStateOf(
            initial.dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        )
    }
    var expanded by remember { mutableStateOf(false) }

    // keep a working draft to pass back
    var draft by remember { mutableStateOf(initial) }
    LaunchedEffect(vendor, totalText, dateText) {
        val amt = totalText.replace(',', '.').toBigDecimalOrNull() ?: initial.totalRon
        draft = draft.copy(
            vendor = vendor,
            totalRon = amt
            // date stays same (read-only text for now)
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text("Confirm receipt", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            // Vendor with suggestions
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = vendor,
                    onValueChange = { vendor = it },
                    label = { Text("Vendor") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    suggestions
                        .filter { it.contains(vendor, ignoreCase = true) }
                        .take(8)
                        .forEach { v ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(v) },
                                onClick = { vendor = v; expanded = false }
                            )
                        }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = totalText,
                onValueChange = { totalText = it },
                label = { Text("Total (RON)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = dateText,
                onValueChange = { /* add date picker later if you want */ },
                readOnly = true,
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Discard")
                }
                Button(
                    onClick = {
                        // ensure we pass correct amount even if user typed comma
                        val amt = totalText.replace(',', '.').toBigDecimalOrNull() ?: BigDecimal.ZERO
                        onSave(draft.copy(
                            vendor = vendor.ifBlank { "Unknown" },
                            totalRon = amt
                        ))
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
