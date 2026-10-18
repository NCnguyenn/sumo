package com.aistudy.myapplication.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ControllerColors = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF092A25),
    secondary = Color(0xFFA6B6CB),
    background = Color(0xFF0B1220),
    surface = Color(0xFF172338),
    surfaceContainer = Color(0xFF172338),
    surfaceVariant = Color(0xFF24344C),
    onSurface = Color(0xFFF4F7FB),
    onSurfaceVariant = Color(0xFFA8B5C8),
    outline = Color(0xFF3D536D),
    error = Color(0xFFFF8790)
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme=ControllerColors, typography=Typography, content=content)
}
