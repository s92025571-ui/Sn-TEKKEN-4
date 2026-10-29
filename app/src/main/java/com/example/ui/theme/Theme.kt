package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SnTekkenColorScheme = darkColorScheme(
    primary = TigerFlameOrange,
    onPrimary = ArenaObsidian,
    primaryContainer = TigerCrimson,
    onPrimaryContainer = TextPure,
    secondary = BuffaloSteelCyan,
    onSecondary = ArenaObsidian,
    secondaryContainer = ArenaSurfaceElevated,
    onSecondaryContainer = TextPure,
    tertiary = TigerGold,
    onTertiary = ArenaObsidian,
    background = ArenaObsidian,
    onBackground = TextPure,
    surface = ArenaDeepSlate,
    onSurface = TextPure,
    surfaceVariant = ArenaSurface,
    onSurfaceVariant = TextSecondary,
    outline = ArenaBorderSteel,
    error = DragonMagmaRed,
    onError = TextPure
)

@Composable
fun SnTekkenTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SnTekkenColorScheme,
        typography = Typography,
        content = content
    )
}
