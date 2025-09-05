package com.example.budgetplanner.ui2.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.budgetplanner.R

// Roboto for body text
private val Roboto = FontFamily(
    Font(R.font.roboto_regular, FontWeight.Normal),
)

// NDot for headers (Nothing-inspired)
private val NDot = FontFamily(
    Font(R.font.ndot, FontWeight.Normal),
)

val NothingTypography = Typography(
    // Headlines (screen titles)
    displayLarge  = TextStyle(fontFamily = NDot, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 40.sp),
    headlineLarge = TextStyle(fontFamily = NDot, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 32.sp),
    headlineMedium= TextStyle(fontFamily = NDot, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 28.sp),
    headlineSmall = TextStyle(fontFamily = NDot, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 24.sp),

    // Titles, body, labels → Roboto
    titleLarge    = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Normal, fontSize = 20.sp),
    titleMedium   = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Normal, fontSize = 18.sp),
    titleSmall    = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Normal, fontSize = 16.sp),

    bodyLarge     = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium    = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall     = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Normal, fontSize = 12.sp),

    labelLarge    = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelMedium   = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 11.sp),
    labelSmall    = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 10.sp),
)
