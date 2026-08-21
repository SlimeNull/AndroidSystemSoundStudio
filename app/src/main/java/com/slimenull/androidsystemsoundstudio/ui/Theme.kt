package com.slimenull.androidsystemsoundstudio.ui

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

private val LightColors = lightColorScheme(
    primary = Color(0xFF006A64),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF2E9),
    onPrimaryContainer = Color(0xFF00201E),
    secondary = Color(0xFF4A635F),
    tertiary = Color(0xFF4B607C),
    surface = Color(0xFFF7FAF8),
    surfaceVariant = Color(0xFFDAE5E2),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80D5CD),
    onPrimary = Color(0xFF003734),
    primaryContainer = Color(0xFF00504B),
    secondary = Color(0xFFB1CCC7),
    tertiary = Color(0xFFB3C8E8),
    surface = Color(0xFF101413),
    surfaceVariant = Color(0xFF3F4947),
)

@Composable
fun SystemSoundTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
