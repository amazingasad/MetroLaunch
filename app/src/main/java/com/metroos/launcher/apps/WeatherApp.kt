package com.metroos.launcher.apps

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metroos.launcher.ui.theme.MetroBlue
import com.metroos.launcher.ui.theme.MetroGray
import com.metroos.launcher.ui.theme.MetroPurple
import com.metroos.launcher.ui.theme.MetroTeal
import kotlinx.coroutines.delay

/**
 * Weather app: current conditions hero with an animated weather
 * background and a 7-day typographic forecast strip.
 */
@Composable
fun WeatherApp() {
    var days by remember { mutableStateOf<List<WeatherDay>?>(null) }

    LaunchedEffect(Unit) {
        while (days == null) {
            days = WeatherRepository.fetchForecast()
            if (days == null) delay(2000)
        }
    }

    val today = days?.firstOrNull()
    val isRainy = (today?.code ?: 0) >= 61
    val sky = when {
        today == null -> MetroGray
        isRainy -> Color(0xFF2A3B4D)
        today.code <= 1 -> MetroBlue
        else -> Color(0xFF33506B)
    }

    Box(modifier = Modifier.fillMaxSize().background(sky)) {
        AnimatedWeatherBackdrop(isRainy = isRainy)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))
            Text(
                today?.let { WeatherRepository.glyph(it.code) } ?: "…",
                color = Color.White,
                fontSize = 72.sp,
                fontWeight = FontWeight.Light
            )
            Text(
                "${today?.hi ?: "--"}°",
                color = Color.White,
                fontSize = 96.sp,
                fontWeight = FontWeight.Light
            )
            Text(
                today?.condition ?: "Loading forecast",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 18.sp
            )

            Spacer(Modifier.height(48.dp))

            days?.drop(1)?.take(6)?.forEach { day ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        day.date,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        WeatherRepository.glyph(day.code),
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(0.5f)
                    )
                    Text(
                        "${day.lo}° / ${day.hi}°",
                        color = Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** Drifting translucent orbs; rain mode adds falling streaks. */
@Composable
private fun AnimatedWeatherBackdrop(isRainy: Boolean) {
    val transition = rememberInfiniteTransition(label = "weather")
    val drift by transition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(tween(8000), RepeatMode.Reverse),
        label = "drift"
    )

    val orbColor = if (isRainy) MetroPurple.copy(alpha = 0.30f) else MetroTeal.copy(alpha = 0.25f)
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { translationX = drift * 4f }
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp)
                .size(260.dp)
                .background(orbColor, CircleShape)
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 60.dp)
                .size(320.dp)
                .background(MetroBlue.copy(alpha = 0.22f), CircleShape)
        )
    }

    if (isRainy) {
        val fall by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
            label = "rain"
        )
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = fall * 1200f - 200f }
                .alpha(0.25f)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                repeat(8) {
                    Box(
                        Modifier
                            .padding(start = 24.dp)
                            .width(2.dp)
                            .height(26.dp)
                            .background(Color.White.copy(alpha = 0.6f))
                    )
                }
            }
        }
    }
}
