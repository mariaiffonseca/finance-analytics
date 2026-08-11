package com.mariafonseca.financeanalytics.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// M3 role assignments are derived from FinanceColors (itself derived from the raw
// palette in Color.kt) so the palette has a single pipeline instead of three
// independently-maintained copies.
private val LightColors = lightColorScheme(
    primary = LightFinanceColors.accent,
    onPrimary = Color.White,
    background = LightFinanceColors.background,
    onBackground = LightFinanceColors.text,
    surface = LightFinanceColors.surface,
    onSurface = LightFinanceColors.text,
    surfaceVariant = LightFinanceColors.surface,
    onSurfaceVariant = LightFinanceColors.textSecondary,
    // Uses textSecondary rather than the low-alpha divider token: the divider's
    // ~1.3:1 contrast against the background fails WCAG 1.4.11 (needs 3:1) for a
    // control border like OutlinedButton's.
    outline = LightFinanceColors.textSecondary,
    outlineVariant = LightFinanceColors.dividerStrong,
    secondary = LightFinanceColors.accent,
    secondaryContainer = LightFinanceColors.accentTint,
    onSecondaryContainer = LightFinanceColors.accentDeep,
    error = LightFinanceColors.accentDeep,
    errorContainer = LightFinanceColors.errorTint,
)

private val DarkColors = darkColorScheme(
    primary = DarkFinanceColors.accent,
    onPrimary = Color.White,
    background = DarkFinanceColors.background,
    onBackground = DarkFinanceColors.text,
    surface = DarkFinanceColors.surface,
    onSurface = DarkFinanceColors.text,
    surfaceVariant = DarkFinanceColors.surface,
    onSurfaceVariant = DarkFinanceColors.textSecondary,
    outline = DarkFinanceColors.textSecondary,
    outlineVariant = DarkFinanceColors.dividerStrong,
    secondary = DarkFinanceColors.accent,
    secondaryContainer = DarkFinanceColors.accentTint,
    onSecondaryContainer = DarkFinanceColors.accentDeep,
    error = DarkFinanceColors.accentDeep,
    errorContainer = DarkFinanceColors.errorTint,
)

@Composable
fun FinanceAnalyticsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val financeColors = if (darkTheme) DarkFinanceColors else LightFinanceColors

    CompositionLocalProvider(LocalFinanceColors provides financeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
