package com.example.budgetplanner.ui2.theme

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun BudgetTheme(
    dark: Boolean = true, // Nothing OS look
    content: @Composable () -> Unit
) {
    val colors = NothingDarkColors
    val systemUi = rememberSystemUiController()
    SideEffect {
        systemUi.setSystemBarsColor(colors.background, darkIcons = false)
        systemUi.setNavigationBarColor(colors.background, darkIcons = false)
    }

    MaterialTheme(
        colorScheme = colors,
        typography = NothingTypography,
        shapes = AppShapes,
        content = content
    )
}

@Composable
fun AccentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) { content() }
}

@Composable
fun NeutralButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) { content() }
}

@Composable
fun WhiteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor   = Color.Black,
            disabledContainerColor = Color(0xFFEDEDED),
            disabledContentColor   = Color(0xFF8A8A8A)
        )
    ) { content() }
}