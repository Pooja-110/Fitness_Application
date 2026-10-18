package com.biowellness.app.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val CyanPrimary    = Color(0xFF00E5FF)
val TealAccent     = Color(0xFF1DE9B6)
val PurpleAccent   = Color(0xFF7C4DFF)
val RedAlert       = Color(0xFFFF1744)
val OrangeWarn     = Color(0xFFFF9100)
val GreenGood      = Color(0xFF00E676)
val YellowColor    = Color(0xFFFFEA00)
val BackgroundDark = Color(0xFF050D1A)
val CardDark       = Color(0xFF0F1F3D)
val CardDarker     = Color(0xFF071325)
val TextPrimary    = Color(0xFFE8F4FD)
val TextSecondary  = Color(0xFF8BAFC8)
val TextMuted      = Color(0xFF4A6B82)

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = BackgroundDark,
    primaryContainer = CardDark,
    onPrimaryContainer = CyanPrimary,
    secondary = TealAccent,
    onSecondary = BackgroundDark,
    tertiary = PurpleAccent,
    background = BackgroundDark,
    surface = Color(0xFF0A1628),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = Color(0xFF1A3A5C),
    error = RedAlert
)

val AppTypography = Typography(
    titleLarge  = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleSmall  = TextStyle(fontWeight = FontWeight.Medium, fontSize = 15.sp),
    bodyLarge   = TextStyle(fontSize = 15.sp),
    bodyMedium  = TextStyle(fontSize = 13.sp),
    labelLarge  = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelMedium = TextStyle(fontSize = 11.sp),
    labelSmall  = TextStyle(fontSize = 10.sp)
)

@Composable
fun BioWellnessTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = BackgroundDark.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
