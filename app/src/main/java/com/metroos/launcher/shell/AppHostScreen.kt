package com.metroos.launcher.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metroos.launcher.apps.BrowserApp
import com.metroos.launcher.apps.CalculatorApp
import com.metroos.launcher.apps.DialerApp
import com.metroos.launcher.apps.MessagesApp
import com.metroos.launcher.apps.NotesApp
import com.metroos.launcher.apps.WeatherApp
import com.metroos.launcher.data.BuiltInApp
import com.metroos.launcher.games.ChessGame
import com.metroos.launcher.games.Game2048
import com.metroos.launcher.games.TetrisGame
import com.metroos.launcher.util.edgeGestures

/**
 * Full-screen host for built-in OS apps and games, with an auto-hiding
 * header and in-app MeeGo edge swipes (bottom edge minimizes, top edge
 * opens quick settings, side edges go to the task switcher).
 */
@Composable
fun AppHostScreen(
    route: String,
    onClose: () -> Unit
) {
    val builtIn = BuiltInApp.byRoute(route)
    var headerVisible by remember { mutableStateOf(true) }
    var quickSettingsVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .edgeGestures(
                edgeWidth = 20.dp,
                onBottom = onClose,
                onTop = { quickSettingsVisible = true },
                onLeft = onClose,
                onRight = onClose
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = headerVisible && builtIn != null,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xEE05070E))
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier
                            .clickable(onClick = onClose)
                            .padding(8.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        builtIn?.label ?: "",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        // Tap empty chrome toggles the header; apps keep their own taps.
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { headerVisible = !headerVisible }
            ) {
                when (route) {
                    BuiltInApp.BROWSER.route -> BrowserApp()
                    BuiltInApp.DIALER.route -> DialerApp()
                    BuiltInApp.MESSAGES.route -> MessagesApp()
                    BuiltInApp.WEATHER.route -> WeatherApp()
                    BuiltInApp.NOTES.route -> NotesApp()
                    BuiltInApp.CALCULATOR.route -> CalculatorApp()
                    BuiltInApp.CHESS.route -> ChessGame()
                    BuiltInApp.GAME_2048.route -> Game2048()
                    BuiltInApp.TETRIS.route -> TetrisGame()
                    else -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Unknown module", color = Color.White)
                        }
                    }
                }
            }
        }

        if (quickSettingsVisible) {
            QuickSettingsPanel(onDismiss = { quickSettingsVisible = false })
        }
    }
}
