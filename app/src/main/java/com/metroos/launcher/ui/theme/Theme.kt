package com.metroos.launcher.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Metro accent palette (modernized Windows Phone hues)
val MetroBlue = Color(0xFF0063B1)
val MetroTeal = Color(0xFF00ABA9)
val MetroGreen = Color(0xFF008A00)
val MetroRed = Color(0xFFE51400)
val MetroPurple = Color(0xFF6A00FF)
val MetroOrange = Color(0xFFFA6800)
val MetroMagenta = Color(0xFFD80073)
val MetroLime = Color(0xFFA4C400)
val MetroGray = Color(0xFF4B4B4B)

private val DarkColors = darkColorScheme(
    primary = MetroBlue,
    onPrimary = Color.White,
    background = Color(0xFF05070E),
    surface = Color(0xFF0E1420),
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1B2436),
    onSurfaceVariant = Color(0xFFB8C4D9)
)

private val LightColors = lightColorScheme(
    primary = MetroBlue,
    onPrimary = Color.White,
    background = Color(0xFF101828),
    surface = Color(0xFF1B2436),
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF26314A),
    onSurfaceVariant = Color(0xFFC6D2E6)
)

private fun metroTypography() = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Light, fontSize = 72.sp, color = Color.White),
    displayMedium = TextStyle(fontWeight = FontWeight.Light, fontSize = 48.sp, color = Color.White),
    headlineMedium = TextStyle(fontWeight = FontWeight.Light, fontSize = 30.sp, color = Color.White),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Color.White),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, color = Color.White),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, color = Color(0xFFB8C4D9)),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.5.sp, color = Color(0xFFB8C4D9))
)

@Composable
fun MetroTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = metroTypography(),
        content = content
    )
}

/**
 * Glassmorphism-style translucent panel. True backdrop blurring is not available
 * to regular window content on Android, so we fake it with layered translucency
 * and a hairline border, which reads as glass over the wallpaper.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val fill = if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.38f)
    val border = if (dark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.18f)
    Box(
        modifier
            .background(fill)
            .border(1.dp, border)
    ) {
        content()
    }
}
