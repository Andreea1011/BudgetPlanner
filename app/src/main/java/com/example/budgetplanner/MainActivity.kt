package com.example.budgetplanner

//import com.example.budgetplanner.ui2.theme.BudgetPlannerTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.budgetplanner.ui2.BudgetApp
import com.example.budgetplanner.ui2.home.HomeScreen
import com.example.budgetplanner.ui2.theme.BudgetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BudgetTheme {
                BudgetApp()
            }
        }


    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    HomeScreen(
        onViewSavings = {}, onViewTransactions = {},
        onViewRecurring = {}, onViewExpenses = {}
    )
}


