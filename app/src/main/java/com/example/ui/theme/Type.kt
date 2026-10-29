package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val RussoOneFamily = FontFamily(
    Font(R.font.russo_one, FontWeight.Normal)
)

val RajdhaniFamily = FontFamily(
    Font(R.font.rajdhani, FontWeight.Normal),
    Font(R.font.rajdhani, FontWeight.Medium),
    Font(R.font.rajdhani, FontWeight.Bold)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = RussoOneFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = 1.5.sp
    ),
    displayMedium = TextStyle(
        fontFamily = RussoOneFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = 1.2.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = RussoOneFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = 1.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = RussoOneFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.8.sp
    ),
    titleLarge = TextStyle(
        fontFamily = RussoOneFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.8.sp
    ),
    titleMedium = TextStyle(
        fontFamily = RajdhaniFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.6.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = RajdhaniFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.3.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = RajdhaniFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontFamily = RussoOneFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.9.sp
    ),
    labelMedium = TextStyle(
        fontFamily = RajdhaniFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.6.sp
    )
)
