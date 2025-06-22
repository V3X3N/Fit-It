package com.vcoffee.fit_it_2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6A1B9A),       // Ciemny fiolet
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE1BEE7),  // Jasny fiolet
    onPrimaryContainer = Color(0xFF4A0072),
    secondary = Color(0xFF7B1FA2),      // Fiolet
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1C4E9),
    onSecondaryContainer = Color(0xFF4A148C),
    tertiary = Color(0xFFAB47BC),       // Akcent fioletowy
    onTertiary = Color(0xFFFFFFFF),
    error = Color(0xFFB00020),
    errorContainer = Color(0xFFF9DEDC),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE6E0E9),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    inverseOnSurface = Color(0xFFF4EFF4),
    inverseSurface = Color(0xFF313033),
    inversePrimary = Color(0xFFD0BCFF),
    surfaceTint = Color(0xFF6A1B9A),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBA68C8),        // Jasny fiolet
    onPrimary = Color(0xFF4A0072),
    primaryContainer = Color(0xFF4A148C), // Ciemny fiolet
    onPrimaryContainer = Color(0xFFE1BEE7),
    secondary = Color(0xFFCE93D8),      // Pastelowy fiolet
    onSecondary = Color(0xFF4A0072),
    secondaryContainer = Color(0xFF7B1FA2),
    onSecondaryContainer = Color(0xFFD1C4E9),
    tertiary = Color(0xFFE1BEE7),       // Jasny fiolet
    onTertiary = Color(0xFF4A148C),
    error = Color(0xFFCF6679),
    errorContainer = Color(0xFFB00020),
    onError = Color(0xFF000000),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF252429),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    inverseOnSurface = Color(0xFF1C1B1F),
    inverseSurface = Color(0xFFE6E0E9),
    inversePrimary = Color(0xFF6A1B9A),
    surfaceTint = Color(0xFFD0BCFF),
)

@Composable
fun FitIt2Theme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}