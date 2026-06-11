package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SimChatPrimary,
    onPrimary = SimChatOnPrimary,
    primaryContainer = SimChatPrimaryContainer,
    onPrimaryContainer = SimChatOnPrimaryContainer,
    secondary = SimChatSecondary,
    onSecondary = SimChatOnSecondary,
    secondaryContainer = SimChatSecondaryContainer,
    background = SimChatBackgroundDark,
    surface = SimChatSurfaceDark,
    onBackground = SimChatOnSurfaceDark,
    onSurface = SimChatOnSurfaceDark,
    error = SimChatError,
    onError = SimChatOnError
)

private val LightColorScheme = lightColorScheme(
    primary = SimChatPrimary,
    onPrimary = SimChatOnPrimary,
    primaryContainer = SimChatPrimaryContainer,
    onPrimaryContainer = SimChatOnPrimaryContainer,
    secondary = SimChatSecondary,
    onSecondary = SimChatOnSecondary,
    secondaryContainer = SimChatSecondaryContainer,
    background = SimChatBackgroundLight,
    surface = SimChatSurfaceLight,
    onBackground = SimChatOnSurfaceLight,
    onSurface = SimChatOnSurfaceLight,
    error = SimChatError,
    onError = SimChatOnError
)

@Composable
fun SimChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontName: String = "default",
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getDynamicTypography(fontName),
        content = content
    )
}

// Keep a backward compatibility alias for default files
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    SimChatTheme(darkTheme = darkTheme, content = content)
}
