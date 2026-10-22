package com.metroos.launcher.data

import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Email
import androidx.compose.ui.graphics.vector.ImageVector
import com.metroos.launcher.ui.theme.MetroBlue
import com.metroos.launcher.ui.theme.MetroGray
import com.metroos.launcher.ui.theme.MetroGreen
import com.metroos.launcher.ui.theme.MetroMagenta
import com.metroos.launcher.ui.theme.MetroOrange
import com.metroos.launcher.ui.theme.MetroPurple
import com.metroos.launcher.ui.theme.MetroRed
import com.metroos.launcher.ui.theme.MetroTeal
import com.metroos.launcher.ui.theme.MetroLime

/**
 * Built-in OS apps and games shipped inside the launcher shell.
 * Route strings are used by the shell navigation and tile store.
 */
enum class BuiltInApp(
    val route: String,
    val label: String,
    val color: Color,
    val icon: ImageVector
) {
    BROWSER("app/browser", "Browser", MetroTeal, Icons.Filled.Language),
    DIALER("app/dialer", "Phone", MetroGreen, Icons.Filled.Phone),
    MESSAGES("app/messages", "Messages", MetroMagenta, Icons.Filled.Email),
    WEATHER("app/weather", "Weather", MetroBlue, Icons.Filled.Cloud),
    NOTES("app/notes", "Notes", MetroLime, Icons.Filled.Edit),
    CALCULATOR("app/calculator", "Calculator", MetroGray, Icons.Filled.Calculate),
    CHESS("game/chess", "Chess", MetroPurple, Icons.Filled.GridOn),
    GAME_2048("game/2048", "2048", MetroOrange, Icons.Filled.Apps),
    TETRIS("game/tetris", "Tetris", MetroRed, Icons.Filled.Dashboard);

    companion object {
        fun byRoute(route: String): BuiltInApp? = entries.firstOrNull { it.route == route }
    }
}
