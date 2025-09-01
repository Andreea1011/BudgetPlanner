package com.example.budgetplanner.ui2.recurring

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
                title = { Text("Recurrent expenses") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Month selector
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = { vm.prevMonth() }) { Text("◀") }
                val label = ui.month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))
                    .replaceFirstChar { it.titlecase(Locale.getDefault()) }
                Text(label, style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { vm.nextMonth() }) { Text("▶") }
            }

            // Total (halved)
            Text("Total (shared): ${"%.2f".format(ui.totalRon / 2)} RON", style = MaterialTheme.typography.titleMedium)

            // Rate row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = ui.rateEurRon.toString(),
                    onValueChange = { it.toDoubleOrNull()?.let(vm::updateRate) },
                    label = { Text("EUR→RON rate (BNR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                // optional: a “Fetch BNR” button later
            }

            // Table header
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Name", modifier = Modifier.weight(1f))
                Text("EUR", modifier = Modifier.weight(1f))
                Text("RON", modifier = Modifier.weight(1f))
            }
            Divider()

            val names = listOf("RENT","GAZ","CURENT","DIGI","INTRETINERE")
            names.forEach { name ->
                val pair = ui.rows[name] ?: (null to null)
                var eurText by remember(ui.month, name, pair.first) { mutableStateOf(pair.first?.let { "%.2f".format(it) } ?: "") }
                var ronText by remember(ui.month, name, pair.second) { mutableStateOf(pair.second?.let { "%.2f".format(it) } ?: "") }

                // when one side changes, compute the other (simple two-way)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) }, modifier = Modifier.weight(1f))

                    OutlinedTextField(
                        value = eurText,
                        onValueChange = { new ->
                            eurText = new
                            vm.setEur(name, new)
                            // mirror RON field locally too
                            val eur = new.toDoubleOrNull()
                            ronText = if (eur != null) "%.2f".format(eur * ui.rateEurRon) else ""
                        },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = ronText,
                        onValueChange = { new ->
                            ronText = new
                            vm.setRon(name, new)
                            val ron = new.toDoubleOrNull()
                            eurText = if (ron != null && ui.rateEurRon != 0.0) "%.2f".format(ron / ui.rateEurRon) else ""
                        },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Save row button (small)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { vm.saveRow(name) }) { Text("Save") }
                }

                Divider()
            }

            // Footer totals
            Text("Total EUR (computed): ${
                "%.2f".format(
                    ui.rows.values.mapNotNull { it.first }.sum()
                )
            }   •   Total RON: ${"%.2f".format(ui.totalRon)}")
        }
    }
}
