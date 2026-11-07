package com.gusak.lab.radarnebo.missioncontrol

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** The colours of the launcher icon: a dark navy console with cyan signal. */
private val darkScheme = darkColorScheme(
    primary = Color(0xFF5FD3E8),
    onPrimary = Color(0xFF00363F),
    background = Color(0xFF08121C),
    surface = Color(0xFF08121C),
    surfaceContainer = Color(0xFF10202E),
    surfaceContainerHigh = Color(0xFF152A3B),
    onSurface = Color(0xFFE3EDF3),
    onSurfaceVariant = Color(0xFF93A6B5),
)

private val lightScheme = lightColorScheme(
    primary = Color(0xFF00687A),
    background = Color(0xFFF4F7F9),
    surface = Color(0xFFF4F7F9),
    surfaceContainer = Color(0xFFE8EEF2),
    surfaceContainerHigh = Color(0xFFFFFFFF),
)

@Composable
internal fun MissionControlTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme, content = content)
}
