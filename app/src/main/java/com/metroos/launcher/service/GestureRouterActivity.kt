package com.metroos.launcher.service

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.metroos.launcher.shell.QuickSettingsPanel
import com.metroos.launcher.shell.TaskSwitcherScreen
import com.metroos.launcher.ui.theme.MetroTheme
import kotlinx.coroutines.delay

/**
 * Transparent overlay activity launched by [EdgeGestureService]. It draws
 * the requested OS surface (quick settings / task switcher / home handoff)
 * over the foreground app and finishes itself once dismissed.
 */
class GestureRouterActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent?.action ?: ACTION_FINISH
        setContent {
            MetroTheme {
                RouterOverlay(mode = mode, onDismiss = { finishAndRemoveTask() })
            }
        }
    }

    companion object {
        const val ACTION_HOME = "com.metroos.action.ROUTE_HOME"
        const val ACTION_SWITCHER = "com.metroos.action.ROUTE_SWITCHER"
        const val ACTION_QUICK_SETTINGS = "com.metroos.action.ROUTE_QUICK_SETTINGS"
        const val ACTION_FINISH = "com.metroos.action.ROUTE_FINISH"
    }
}

@Composable
private fun RouterOverlay(mode: String, onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(mode) {
        if (mode != GestureRouterActivity.ACTION_FINISH) {
            delay(40)
            visible = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (mode) {
            GestureRouterActivity.ACTION_QUICK_SETTINGS -> {
                // Scrim + panel; dismissal animates out then finishes.
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                            .clickable(onClick = onDismiss)
                    )
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut()
                ) {
                    QuickSettingsPanel(onDismiss = onDismiss)
                }
            }
            GestureRouterActivity.ACTION_SWITCHER -> {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    TaskSwitcherScreen(
                        onDismiss = onDismiss,
                        onHome = onDismiss
                    )
                }
            }
            GestureRouterActivity.ACTION_HOME -> {
                // Bottom edge over a third-party app: hand control back to
                // the launcher home screen, then close the overlay.
                val ctx = androidx.compose.ui.platform.LocalContext.current
                LaunchedEffect(Unit) {
                    runCatching {
                        ctx.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_MAIN)
                                .addCategory(android.content.Intent.CATEGORY_HOME)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    onDismiss()
                }
            }
            else -> {
                LaunchedEffect(Unit) { onDismiss() }
            }
        }
    }
}
