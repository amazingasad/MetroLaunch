package com.metroos.launcher

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metroos.launcher.data.Tile
import com.metroos.launcher.data.TileSize
import com.metroos.launcher.shell.LiveTile
import com.metroos.launcher.ui.theme.GlassSurface
import com.metroos.launcher.ui.theme.MetroBlue
import com.metroos.launcher.ui.theme.MetroTheme

/**
 * Static design previews matching /design/*.svg mockups. Render in
 * Android Studio or via the CI screenshot job to verify the visual
 * language without a device.
 */
@Preview(name = "Tile grid - dark", showBackground = true, widthDp = 360, heightDp = 640)
@Preview(name = "Tile grid - light", showBackground = true, widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun PreviewTileGrid() {
    MetroTheme {
        Column(
            modifier = Modifier
                .background(Color(0xFF05070E))
                .padding(16.dp)
        ) {
            Text("9:41", color = Color.White, fontSize = 56.sp)
            Text("Thursday, March 14", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LiveTile(
                    tile = Tile("chess", "game/chess", TileSize.MEDIUM, 0xFF6A00FF),
                    modifier = Modifier
                        .width(160.dp)
                        .aspectRatio(2f),
                    onOpenRoute = {}
                )
                LiveTile(
                    tile = Tile("phone", "app/dialer", TileSize.SMALL, 0xFF008A00),
                    modifier = Modifier
                        .size(76.dp),
                    onOpenRoute = {}
                )
            }
            Spacer(Modifier.height(12.dp))
            GlassSurface(modifier = Modifier.fillMaxWidth().height(64.dp)) {
                Box(Modifier.padding(14.dp)) {
                    Text("Glassmorphism surface", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}
