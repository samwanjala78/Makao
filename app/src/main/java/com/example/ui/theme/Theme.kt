package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = KenyaForestGreenLight,
    onPrimary = Color.White,
    primaryContainer = KenyaForestGreen,
    onPrimaryContainer = Color.White,
    secondary = KenyaGoldAccent,
    onSecondary = Color.Black,
    secondaryContainer = KenyaGoldAccentDark,
    onSecondaryContainer = Color.White,
    tertiary = KenyaGoldAccentLight,
    background = DarkBg,
    onBackground = TextLightPrimary,
    surface = DarkSurface,
    onSurface = TextLightPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextLightSecondary,
    outline = Color(0xFF334155)
)

private val LightColorScheme = lightColorScheme(
    primary = KenyaForestGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2F3ED),
    onPrimaryContainer = KenyaForestGreenDark,
    secondary = KenyaGoldAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = KenyaGoldAccentDark,
    tertiary = KenyaForestGreenLight,
    background = OffWhiteBg,
    onBackground = TextDarkPrimary,
    surface = CardSurfaceLight,
    onSurface = TextDarkPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextDarkSecondary,
    outline = DividerLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our tailored Kenyan Makao theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
