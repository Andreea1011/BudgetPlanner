package com.example.budgetplanner.ui2.savings

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Totals
            Text(
                "Total savings: ${"%.2f".format(ui.totalRon)} RON" +
                        (ui.totalEur?.let { "  •  ${"%.2f".format(it)} EUR" } ?: ""),
                style = MaterialTheme.typography.titleMedium
            )

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { vm.openAddDialog() }) { Text("Insert savings source") }
            }

            // Table header
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Name", modifier = Modifier.weight(1f))
                Text("RON", modifier = Modifier.width(120.dp))
                Text("EUR", modifier = Modifier.width(120.dp))
                Spacer(Modifier.width(48.dp)) // for delete icon
            }
            Divider()

            // Table
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ui.rows, key = { it.id }) { row ->
                    var ronText by remember(row.id, row.amountRon) {
                        mutableStateOf(String.format(Locale.getDefault(), "%.2f", row.amountRon))
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(row.name, modifier = Modifier.weight(1f))

                        OutlinedTextField(
                            value = ronText,
                            onValueChange = { new ->
                                ronText = new
                                new.toDoubleOrNull()?.let { vm.updateAmount(row.id, it) }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(120.dp)
                        )

                        Text(
                            row.amountEur?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "-",
                            modifier = Modifier.width(120.dp)
                        )

                        IconButton(onClick = { vm.delete(row.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                    Divider()
                }
            }
        }
    }

    if (ui.showAddDialog) {
        AddPotDialog(
            onDismiss = { vm.closeAddDialog() },
            onConfirm = { name, amount ->
                vm.addPot(name, amount)
            }
        )
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
                onClick = { onConfirm(name.trim(), ron.toDouble()) }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
