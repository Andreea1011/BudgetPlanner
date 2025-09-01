package com.example.budgetplanner.ui2.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    currentBalanceRon: String,
    totalSavingsRon: String,
    onViewSavings: () -> Unit,
    onViewTransactions: () -> Unit,
    onViewRecurring: () -> Unit,
    onViewExpenses: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Current Balance and Total Savings
        Text(
            text = "Current balance: $currentBalanceRon",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Total savings: $totalSavingsRon",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        // Buttons for navigation
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onViewSavings,
                modifier = Modifier.weight(1f)
            ) { Text("View all savings") }

            Button(
                onClick = onViewTransactions,
                modifier = Modifier.weight(1f)
            ) { Text("View all transactions") }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onViewRecurring,
                modifier = Modifier.weight(1f)
            ) { Text("View recurrent transactions") }

            Button(
                onClick = onViewExpenses,
                modifier = Modifier.weight(1f)
            ) { Text("View all expenses") }
        }

        // Charts (placeholders for now)
        Spacer(Modifier.height(16.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("Chart with spendings by category")
            }
        }

        Spacer(Modifier.height(16.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("Chart with total monthly spendings")
            }
        }
    }
}
