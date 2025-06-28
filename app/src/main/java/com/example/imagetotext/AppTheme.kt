package com.example.imagetotext

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.imagetotext.ui.theme.Typography
import kotlin.text.Typography

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6366F1),
    secondary = Color(0xFF8B5CF6),
    surface = Color(0xFF1E1E1E),
    background = Color(0xFF121212),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onSurface = Color.White,
    onBackground = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6366F1),
    secondary = Color(0xFF8B5CF6),
    surface = Color(0xFFF8FAFC),
    background = Color(0xFFF1F5F9),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onSurface = Color(0xFF1E293B),
    onBackground = Color(0xFF1E293B)
)

@Composable
fun ImageToTextTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
