package com.metroos.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.metroos.launcher.data.AppInfo
import com.metroos.launcher.data.RecentApp
import com.metroos.launcher.data.RecentsStore
import com.metroos.launcher.shell.AppHostScreen
import com.metroos.launcher.shell.AppDrawerScreen
import com.metroos.launcher.shell.HomeScreen
import com.metroos.launcher.shell.QuickSettingsPanel
import com.metroos.launcher.shell.TaskSwitcherScreen
import com.metroos.launcher.ui.theme.MetroTheme
import com.metroos.launcher.util.edgeGestures

/** Shell destinations. Built-in apps open in-app; external apps launch as system intents. */
sealed interface ShellRoute {
    data object Home : ShellRoute
    data object Drawer : ShellRoute
    data object Switcher : ShellRoute
    data class InApp(val route: String) : ShellRoute
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MetroTheme {
                LauncherShell()
            }
        }
    }

    // Re-entering from HOME button: simply bring the shell to front.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun LauncherShell() {
    var route by remember { mutableStateOf<ShellRoute>(ShellRoute.Home) }
    var showQuickSettings by remember { mutableStateOf(false) }

    val openInApp: (String) -> Unit = { r -> route = ShellRoute.InApp(r) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val launchExternal: (AppInfo) -> Unit = { app ->
        RecentsStore.push(RecentApp(app.packageName, app.label, app.launchIntent))
        runCatching { context.startActivity(app.launchIntent) }
    }

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .edgeGestures(
                // Bottom edge swipe: minimize active app / return home.
                onBottom = { route = ShellRoute.Home },
                // Top edge swipe: unified quick settings.
                onTop = { showQuickSettings = true },
                // Side edges: slide into the task switcher.
                onLeft = { route = ShellRoute.Switcher },
                onRight = { route = ShellRoute.Switcher }
            )
    ) {
        AnimatedContent(
            targetState = route,
            transitionSpec = {
                (slideInVertically(tween(260)) { it / 6 } + fadeIn(tween(220)))
                    .togetherWith(slideOutVertically(tween(200)) { -it / 8 } + fadeOut(tween(160)))
            },
            label = "shell"
        ) { current ->
            when (current) {
                ShellRoute.Home -> HomeScreen(
                    onOpenRoute = openInApp,
                    onOpenDrawer = { route = ShellRoute.Drawer },
                    onLaunchApp = launchExternal
                )
                ShellRoute.Drawer -> AppDrawerScreen(
                    onBack = { route = ShellRoute.Home },
                    onLaunchApp = launchExternal
                )
                ShellRoute.Switcher -> TaskSwitcherScreen(
                    onDismiss = { route = ShellRoute.Home },
                    onHome = { route = ShellRoute.Home }
                )
                is ShellRoute.InApp -> AppHostScreen(
                    route = current.route,
                    onClose = { route = ShellRoute.Home }
                )
            }
        }

        if (showQuickSettings) {
            QuickSettingsPanel(onDismiss = { showQuickSettings = false })
        }
    }
}
