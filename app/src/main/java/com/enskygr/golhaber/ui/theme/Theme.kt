package com.enskygr.golhaber.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object GH {
    val Green = Color(0xFF2EE577)
    val GreenDeep = Color(0xFF12B856)
    val Live = Color(0xFFFF3B47)
    val Gold = Color(0xFFFFC53D)
    val Bg = Color(0xFF0A0F0D)
    val Surface = Color(0xFF121A16)
    val Surface2 = Color(0xFF1A2620)
    val Line = Color(0xFF243229)
    val Text = Color(0xFFE8F0EC)
    val Muted = Color(0xFF8A9A92)
}

private val scheme = darkColorScheme(
    primary = GH.Green,
    onPrimary = Color(0xFF04140A),
    primaryContainer = GH.GreenDeep,
    onPrimaryContainer = Color.White,
    secondary = GH.Gold,
    onSecondary = Color(0xFF1C1400),
    background = GH.Bg,
    onBackground = GH.Text,
    surface = GH.Surface,
    onSurface = GH.Text,
    surfaceVariant = GH.Surface2,
    onSurfaceVariant = GH.Muted,
    surfaceContainer = GH.Surface,
    surfaceContainerHigh = GH.Surface2,
    surfaceContainerHighest = GH.Surface2,
    outline = GH.Line,
    outlineVariant = GH.Line,
    error = GH.Live,
)

@Composable
fun GolHaberTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}
