package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BlueAccent,
    onPrimary = Color.White,
    secondary = Slate400,
    onSecondary = Color.White,
    background = Slate950,
    surface = Slate900,
    onBackground = Slate50,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate200,
    error = RoseAccent,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = BlueAccent,
    onPrimary = Color.White,
    secondary = Slate600,
    onSecondary = Color.White,
    background = Slate50,
    surface = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    error = RoseAccent,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Strictly FULL CLEAN WHITE / LIGHT THEME
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Pure White / Light theme enforced throughout entire application
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
