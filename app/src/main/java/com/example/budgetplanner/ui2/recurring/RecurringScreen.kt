package com.example.budgetplanner.ui2.recurring

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as Application
    val vm: RecurringViewModel = viewModel(factory = RecurringVMFactory(app))
    val ui by vm.ui.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Recurrent expenses", style = MaterialTheme.typography.headlineLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Month selector
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { vm.prevMonth() }) { Text("◀") }
                val label = ui.month.format(
                    DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault())
                ).replaceFirstChar { it.titlecase(Locale.getDefault()) }
                Text(label, style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { vm.nextMonth() }) { Text("▶") }
            }

            // Rate + fetch
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = String.format(Locale.getDefault(), "%.4f", ui.rateEurRon),
                    onValueChange = { txt -> txt.toDoubleOrNull()?.let(vm::updateRate) },
                    label = { Text("EUR → RON") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { vm.fetchRate() }) { Text("Fetch rate") }
            }

            // Totals
            Text(
                "Total (shared): ${"%.2f".format(ui.totalRon / 2)} RON",
                style = MaterialTheme.typography.titleMedium
            )

            // Header
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Name", modifier = Modifier.weight(1f))
                Text("EUR", modifier = Modifier.weight(1f))
                Text("RON", modifier = Modifier.weight(1f))
            }
            Divider()

            // Scrollable rows
            val order = listOf("RENT", "GAZ", "CURENT", "DIGI", "INTRETINERE")
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
            ) {
                items(order) { name ->
                    val base = ui.rows[name]  // Pair<Double?, Double?> or null

                    // Local text state, initialized from VM ONLY when month or row changes
                    var eurText by remember(ui.month, name, base?.first) {
                        mutableStateOf(base?.first?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "")
                    }
                    var ronText by remember(ui.month, name, base?.second) {
                        mutableStateOf(base?.second?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "")
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) },
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = eurText,
                                onValueChange = { txt ->
                                    eurText = txt
                                    txt.toDoubleOrNull()?.let { eur ->
                                        // mirror locally if valid
                                        ronText = String.format(Locale.getDefault(), "%.2f", eur * ui.rateEurRon)
                                    }
                                },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = ronText,
                                onValueChange = { txt ->
                                    ronText = txt
                                    txt.toDoubleOrNull()?.let { ron ->
                                        if (ui.rateEurRon != 0.0) {
                                            val eur = ron / ui.rateEurRon
                                            eurText = String.format(Locale.getDefault(), "%.2f", eur)
                                        }
                                    }
                                },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = {
                                    // Save what’s typed right now
                                    vm.saveRowImmediate(
                                        name = name,
                                        eurText = eurText,
                                        ronText = ronText
                                    )
                                }
                            ) { Text("Save") }
                        }
                        Divider()
                    }
                }
            }

            // Footer totals
            val totalRon = ui.totalRon
            val totalEur = ui.rows.values.mapNotNull { it.first }.sum()
            Text("Total EUR (computed): ${"%.2f".format(totalEur)}   •   Total RON: ${"%.2f".format(totalRon)}")

            ui.error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
        }
    }
}
