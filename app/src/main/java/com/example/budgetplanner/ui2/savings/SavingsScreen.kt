package com.example.budgetplanner.ui2.savings

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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
                title = { Text("Savings", style = MaterialTheme.typography.headlineLarge) },
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
                Text("Name", modifier = Modifier.weight(1.6f))
                Text("RON", modifier = Modifier.weight(0.9f))
                Text("EUR", modifier = Modifier.weight(0.9f))
                Spacer(Modifier.width(40.dp))
            }
            Divider(Modifier.padding(top = 2.dp, bottom = 6.dp))

            // List – Mom surplus row first (if exists), then other pots
            val (momRows, otherRows) = ui.rows.partition { it.name.equals("Mom surplus", true) }
            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            )  {
                if (momRows.isNotEmpty()) {
                    item(key = "mom_row") {
                        SavingsRow(
                            row = momRows.first(),
                            onSet = vm::updateAmount,
                            onDelete = vm::delete,
                            highlight = true,
                            deletable = false               // keep mom pot
                        )
                        Divider(Modifier.padding(top = 6.dp))
                    }
                }
                items(otherRows, key = { it.id }) { row ->
                    SavingsRow(
                        row = row,
                        onSet = vm::updateAmount,
                        onDelete = vm::delete
                    )
                    Divider(Modifier.padding(top = 6.dp))
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
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            row.name,
            modifier = Modifier.weight(1.6f),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
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
            modifier = Modifier.weight(0.9f)
        )

        Text(
            row.amountEur?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "-",
            modifier = Modifier.weight(0.9f),
            style = MaterialTheme.typography.bodyLarge
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
