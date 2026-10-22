package com.metroos.launcher.shell

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metroos.launcher.ui.theme.GlassSurface
import com.metroos.launcher.ui.theme.MetroBlue
import com.metroos.launcher.ui.theme.MetroTeal
import kotlin.math.roundToInt

/**
 * Unified translucent quick-settings panel pulled down by the top edge
 * swipe. Works both inside the shell and from the gesture-router overlay.
 */
@Composable
fun QuickSettingsPanel(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var torchOn by remember { mutableStateOf(false) }
    var brightness by remember {
        mutableFloatStateOf(
            try {
                Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                    .toFloat() / 255f
            } catch (_: Exception) {
                0.5f
            }
        )
    }

    // Dim scrim behind the panel; tap or swipe up to dismiss.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onDismiss)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    if (change.position.y > size.height * 0.6f) {
                        onDismiss()
                    }
                }
            }
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                val batteryPct = remember {
                    val sticky = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    val level = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                    val scale = sticky?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
                    if (level >= 0) level * 100 / scale else 100
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Quick settings",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "$batteryPct% battery",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(18.dp))

                // Toggle grid
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToggleTile("Wi-Fi", "Open Wi-Fi settings", MetroBlue, Modifier.weight(1f)) {
                        runCatching {
                            context.startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
                        }
                    }
                    ToggleTile("Bluetooth", "Open Bluetooth settings", MetroBlue, Modifier.weight(1f)) {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToggleTile(
                        "Torch",
                        if (torchOn) "On" else "Off",
                        if (torchOn) MetroTeal else Color(0xFF333333),
                        Modifier.weight(1f)
                    ) {
                        torchOn = toggleTorch(context, torchOn)
                    }
                    ToggleTile("Do not disturb", if (isDnd(context)) "On" else "Off", Color(0xFF333333), Modifier.weight(1f)) {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text("Brightness", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Slider(
                    value = brightness,
                    onValueChange = {
                        brightness = it
                        if (Settings.System.canWrite(context)) {
                            runCatching {
                                Settings.System.putInt(
                                    context.contentResolver,
                                    Settings.System.SCREEN_BRIGHTNESS,
                                    (it * 255f).roundToInt().coerceIn(1, 255)
                                )
                            }
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = MetroTeal,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
                if (!Settings.System.canWrite(context)) {
                    Text(
                        "Tap here then allow \"Modify system settings\" to control brightness.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        modifier = Modifier.clickable {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleTile(
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .background(color)
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Column {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

private fun toggleTorch(context: Context, currentlyOn: Boolean): Boolean {
    return runCatching {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return false
        manager.setTorchMode(cameraId, !currentlyOn)
        !currentlyOn
    }.getOrDefault(currentlyOn)
}

private fun isDnd(context: Context): Boolean = runCatching {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
}.getOrDefault(false)
