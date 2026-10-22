package com.metroos.launcher.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** Color <-> hex helpers shared by tile persistence. */
fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    return String.format("%08X", argb)
}

fun hexToColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex"))
} catch (_: Exception) {
    Color(0xFF0063B1)
}
