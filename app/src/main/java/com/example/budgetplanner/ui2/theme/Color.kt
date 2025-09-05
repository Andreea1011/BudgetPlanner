package com.example.budgetplanner.ui2.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Nothing-ish palette (dark)
val NoRed            = Color(0xFFFF2B2B)   // accent red
val NoRedContainer   = Color(0xFF5A1212)   // dark red container
val NoOnRed          = Color(0xFFFFFFFF)

val NoGraphite900    = Color(0xFF0F0F11)   // window background
val NoGraphite800    = Color(0xFF161619)   // surfaces
val NoGraphite700    = Color(0xFF1D1D21)   // surfaceContainer
val NoOnGraphite     = Color(0xFFEDEDED)   // primary text
val NoOnGraphiteDim  = Color(0xFFB9B9B9)   // secondary text
val NoOutline        = Color(0xFF2C2C31)   // dividers/outline

// Material3 color scheme (dark)
val NothingDarkColors = darkColorScheme(
    primary              = NoRed,
    onPrimary            = NoOnRed,
    primaryContainer     = NoRedContainer,
    onPrimaryContainer   = NoOnRed,

    secondary            = NoOnGraphite,          // neutral
    onSecondary          = Color(0xFF121212),
    secondaryContainer   = NoGraphite700,
    onSecondaryContainer = NoOnGraphite,

    background           = NoGraphite900,
    onBackground         = NoOnGraphite,

    surface              = NoGraphite800,
    onSurface            = NoOnGraphite,
    surfaceVariant       = NoGraphite700,
    onSurfaceVariant     = NoOnGraphiteDim,

    outline              = NoOutline
)
