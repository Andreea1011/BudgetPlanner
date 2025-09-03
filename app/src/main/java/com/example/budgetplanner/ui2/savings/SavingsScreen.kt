package com.example.budgetplanner.ui2.savings

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as Application
    val vm: SavingsViewModel = viewModel(factory = SavingsVMFactory(app))
    val ui by vm.ui.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Savings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = { TextButton(onClick = vm::openAddDialog) { Text("Add") } }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Totals
            Text(
                "Total savings: ${"%.2f".format(ui.totalRon)} RON" +
                        (ui.totalEur?.let { "  •  ${"%.2f".format(it)} EUR" } ?: ""),
                style = MaterialTheme.typography.titleMedium
            )

            // Headers
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Name", modifier = Modifier.weight(1f))
                Text("RON", modifier = Modifier.width(120.dp))
                Text("EUR", modifier = Modifier.width(120.dp))
                Spacer(Modifier.width(48.dp)) // delete icon space
            }
            Divider()

            // List – Mom surplus row first (if exists), then other pots
            val (momRows, otherRows) = ui.rows.partition { it.name.equals("Mom surplus", true) }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (momRows.isNotEmpty()) {
                    item(key = "mom_row") {
                        SavingsRow(
                            row = momRows.first(),
                            onSet = vm::updateAmount,
                            onDelete = vm::delete,
                            highlight = true,
                            deletable = false               // keep mom pot
                        )
                        Divider()
                    }
                }
                items(otherRows, key = { it.id }) { row ->
                    SavingsRow(
                        row = row,
                        onSet = vm::updateAmount,
                        onDelete = vm::delete
                    )
                    Divider()
                }
            }
        }
    }

    if (ui.showAddDialog) {
        AddPotDialog(
            onDismiss = vm::closeAddDialog,
            onConfirm = { name, amount -> vm.addPot(name, amount) }
        )
    }
}

@Composable
private fun SavingsRow(
    row: SavingsRowUi,
    onSet: (Long, Double) -> Unit,
    onDelete: (Long) -> Unit,
    highlight: Boolean = false,
    deletable: Boolean = true
) {
    var ronText by remember(row.id, row.amountRon) {
        mutableStateOf(String.format(Locale.getDefault(), "%.2f", row.amountRon))
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            row.name,
            modifier = Modifier.weight(1f),
            style = if (highlight) MaterialTheme.typography.titleMedium else LocalTextStyle.current
        )

        OutlinedTextField(
            value = ronText,
            onValueChange = { new ->
                ronText = new
                new.toDoubleOrNull()?.let { onSet(row.id, it) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.width(120.dp)
        )

        Text(
            row.amountEur?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "-",
            modifier = Modifier.width(120.dp)
        )

        IconButton(
            onClick = { if (deletable) onDelete(row.id) },
            enabled = deletable
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = if (deletable) "Delete" else "Protected"
            )
        }
    }
}

@Composable
private fun AddPotDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ron by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New savings source") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = ron,
                    onValueChange = { ron = it },
                    label = { Text("Amount (RON)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && ron.toDoubleOrNull() != null,
                onClick = {
                    onConfirm(name.trim(), ron.toDouble())
                }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
