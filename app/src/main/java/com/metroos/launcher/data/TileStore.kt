package com.metroos.launcher.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.metroos.launcher.util.Prefs
import org.json.JSONArray
import org.json.JSONObject

enum class TileSize(val cols: Int, val rows: Int) {
    SMALL(1, 1), MEDIUM(2, 1), WIDE(2, 2)
}

data class Tile(
    val id: String,
    val route: String,
    val size: TileSize = TileSize.SMALL,
    val color: Long = 0xFF0063B1
) {
    val colorValue: Color get() = Color(color)
}

/**
 * Persistent live-tile layout. A tile is a (route, size, color) triple;
 * live data (clock, weather, news) is bound by the tile renderer, not stored here.
 */
object TileStore {

    private const val KEY = "tiles_json"
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun defaultTiles(): List<Tile> = listOf(
        Tile("clock", "tile/clock", TileSize.WIDE, 0xFF0063B1),
        Tile("weather", "tile/weather", TileSize.MEDIUM, 0xFF00ABA9),
        Tile("browser", "app/browser", TileSize.MEDIUM, 0xFF4B4B4B),
        Tile("phone", "app/dialer", TileSize.SMALL, 0xFF008A00),
        Tile("messages", "app/messages", TileSize.SMALL, 0xFFD80073),
        Tile("chess", "game/chess", TileSize.MEDIUM, 0xFF6A00FF),
        Tile("g2048", "game/2048", TileSize.SMALL, 0xFFFA6800),
        Tile("tetris", "game/tetris", TileSize.SMALL, 0xFFE51400),
        Tile("notes", "app/notes", TileSize.SMALL, 0xFFA4C400),
        Tile("calc", "app/calculator", TileSize.SMALL, 0xFF333333)
    )

    fun load(): List<Tile> {
        if (!::appContext.isInitialized) return defaultTiles()
        val raw = Prefs.getString(appContext, KEY, "")
        if (raw.isBlank()) return defaultTiles()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                Tile(
                    id = o.optString("id"),
                    route = o.optString("route"),
                    size = TileSize.entries.firstOrNull { it.name == o.optString("size") }
                        ?: TileSize.SMALL,
                    color = o.optLong("color", 0xFF0063B1L)
                )
            }.ifEmpty { defaultTiles() }
        } catch (_: Exception) {
            defaultTiles()
        }
    }

    fun save(tiles: List<Tile>) {
        if (!::appContext.isInitialized) return
        val arr = JSONArray()
        tiles.forEach { t ->
            arr.put(
                JSONObject()
                    .put("id", t.id)
                    .put("route", t.route)
                    .put("size", t.size.name)
                    .put("color", t.color)
            )
        }
        Prefs.putString(appContext, KEY, arr.toString())
    }
}

/** Long -> Color helper for persisted tile colors. */
fun tileColor(color: Long): Color = Color(color)

/** Color -> packed ARGB Long helper for persistence. */
fun packColor(color: Color): Long = color.toArgb().toLong() and 0xFFFFFFFFL
