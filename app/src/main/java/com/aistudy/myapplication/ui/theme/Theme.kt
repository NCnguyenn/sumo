package com.aistudy.myapplication.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ControllerColors = darkColorScheme(
    primary = Color(0xFF6BE6CC),
    onPrimary = Color(0xFF092A25),
    secondary = Color(0xFFA6B6CB),
    background = Color(0xFF0B1016),
    surface = Color(0xFF131C27),
    surfaceContainer = Color(0xFF182330),
    surfaceVariant = Color(0xFF243142),
    onSurface = Color(0xFFF1F5FA),
    onSurfaceVariant = Color(0xFF9EADC0),
    outline = Color(0xFF46596D),
    error = Color(0xFFFF8790)
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme=ControllerColors, typography=Typography, content=content)
}
