package com.example.budgetplanner.ui2.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.budgetplanner.domain.model.Category
import com.example.budgetplanner.domain.model.Transaction
import com.example.budgetplanner.ui2.transactions.CreditAllocation
import com.example.budgetplanner.ui2.transactions.ExpenseCoverage
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionBottomSheet(
    tx: Transaction,
    onDismiss: () -> Unit,
    onSaveNoteAndCategory: (note: String?, category: Category) -> Unit,
    onToggleExclude: (checked: Boolean) -> Unit,
    onToggleMom: (checked: Boolean) -> Unit,
    onDelete: () -> Unit,

    expenseCoverage: ExpenseCoverage? = null,
    creditAllocation: CreditAllocation? = null
) {
    // state
    var note by remember(tx.id) { mutableStateOf(TextFieldValue(tx.note ?: "")) }
    var cat  by remember(tx.id) { mutableStateOf(tx.category) }
    var exclude by remember(tx.id) { mutableStateOf(tx.excludePersonal) }
    var fromMom by remember(tx.id) { mutableStateOf(tx.party == "MOM") }

    // formatters
    val zone = remember { ZoneId.systemDefault() }
    val dateStr = remember(tx.timestamp) {
        Instant.ofEpochMilli(tx.timestamp).atZone(zone).toLocalDate()
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy, EEEE", Locale.getDefault()))
    }
    val amountStr = remember(tx.amountRon) {
        val s = if (tx.amountRon < 0) "-" else "+"
        "$s${"%.2f".format(Locale.getDefault(), abs(tx.amountRon))} RON"
    }

    // controlled sheet (prevents crash while dismissing)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun safeDismiss(block: () -> Unit = {}) {
        scope.launch {
            try { sheetState.hide() } finally {
                block()
                onDismiss()
            }
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { safeDismiss() }
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {

            Text(text = tx.merchant ?: "Details", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text("Date: $dateStr", style = MaterialTheme.typography.bodyMedium)
            Text("Amount: $amountStr", style = MaterialTheme.typography.bodyMedium)

            Divider(Modifier.padding(vertical = 12.dp))

            Text("Category", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            CategoryDropdown(current = cat, onSelect = { cat = it })

            Spacer(Modifier.height(12.dp))

            Text("Note", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth()
            )

            Divider(Modifier.padding(vertical = 12.dp))

            // If it's an expense and we have coverage info
            expenseCoverage?.let { info ->
                Text(
                    "Covered ${"%.2f".format(info.covered)} / ${"%.2f".format(info.total)}  →  Open ${"%.2f".format(info.open)} RON",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
            }

// If it's a credit and we have allocation info
            creditAllocation?.let { a ->
                Text(
                    "Allocated ${"%.2f".format(a.allocated)} RON to ${a.items} item(s)  •  Surplus ${"%.2f".format(a.surplus)} RON",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
            }

            Divider(Modifier.padding(vertical = 12.dp))

// Exclude toggle — only for EXPENSES (negative)
            if (tx.originalAmount < 0) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Exclude from personal spend")
                    Switch(
                        checked = exclude,
                        onCheckedChange = {
                            exclude = it
                            onToggleExclude(it) // persist immediately
                        }
                    )
                }
            }

// Mom toggle — only for CREDITS (positive)
            if (tx.originalAmount > 0) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Transaction from Mom?")
                    Switch(
                        checked = fromMom,
                        onCheckedChange = {
                            fromMom = it
                            onToggleMom(it) // persist immediately
                        }
                    )
                }
            }



            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { safeDismiss { onSaveNoteAndCategory(note.text.ifBlank { null }, cat) } }
                ) { Text("Save") }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { safeDismiss() }
                ) { Text("Close") }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { safeDismiss { onDelete() } },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Delete Transaction") }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    current: Category,
    onSelect: (Category) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = current.name,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            label = { Text("Category") }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Category.entries.forEach { c ->
                DropdownMenuItem(text = { Text(c.name) }, onClick = {
                    onSelect(c); expanded = false
                })
            }
        }
    }
}
