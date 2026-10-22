package com.metroos.launcher.apps

import android.annotation.SuppressLint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

data class WeatherDay(
    val date: String,
    val hi: Int,
    val lo: Int,
    /** WMO weather code from Open-Meteo. */
    val code: Int
) {
    val condition: String get() = describe(code)
}

/**
 * Weather repository backed by Open-Meteo (no API key required).
 * Falls back to a stable sample forecast when offline so tiles and the
 * weather app always render.
 */
object WeatherRepository {

    private const val SUMMARY_KEY = "weather_summary_json"

    data class Summary(val day: String, val hi: Int, val lo: Int, val condition: String)

    @SuppressLint("DefaultLocale")
    suspend fun fetchSummary(): Summary = withContext(Dispatchers.IO) {
        val days = fetchForecast()
        val first = days.firstOrNull() ?: sample().first()
        Summary(first.date, first.hi, first.lo, first.condition)
    }

    suspend fun fetchForecast(
        latitude: Double = 40.71,
        longitude: Double = -74.01
    ): List<WeatherDay> = withContext(Dispatchers.IO) {
        try {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$latitude&longitude=$longitude" +
                    "&daily=temperature_2m_max,temperature_2m_min,weathercode" +
                    "&timezone=auto&forecast_days=7"
            )
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            parse(body)
        } catch (_: Exception) {
            sample()
        }
    }

    fun parse(json: String): List<WeatherDay> {
        val root = JSONObject(json)
        val daily = root.getJSONObject("daily")
        val dates = daily.getJSONArray("time")
        val max = daily.getJSONArray("temperature_2m_max")
        val min = daily.getJSONArray("temperature_2m_min")
        val codes = daily.getJSONArray("weathercode")
        val out = mutableListOf<WeatherDay>()
        for (i in 0 until dates.length()) {
            val raw = dates.getString(i)
            val day = try {
                LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
            } catch (_: Exception) {
                raw
            }
            out.add(
                WeatherDay(
                    date = day,
                    hi = max.getDouble(i).roundToInt(),
                    lo = min.getDouble(i).roundToInt(),
                    code = codes.getInt(i)
                )
            )
        }
        return out
    }

    /** Stable pseudo-random sample used offline. */
    fun sample(): List<WeatherDay> {
        val seed = Instant.now().epochSecond / 86_400
        val base = listOf(21, 24, 19, 22, 25, 20, 23)
        return base.mapIndexed { i, hi ->
            WeatherDay(
                date = LocalDate.now().plusDays(i.toLong())
                    .format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())),
                hi = hi + (seed % 3).toInt(),
                lo = hi - 7,
                code = when (i % 3) {
                    0 -> 0      // clear
                    1 -> 2      // partly cloudy
                    else -> 61  // rain
                }
            )
        }
    }

    fun describe(code: Int): String = when (code) {
        0 -> "Clear sky"
        1, 2 -> "Partly cloudy"
        3 -> "Overcast"
        in 45..48 -> "Fog"
        in 51..57 -> "Drizzle"
        in 61..67 -> "Rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Showers"
        else -> "Cloudy"
    }

    /** Emoji-free weather glyph selector for tiles. */
    fun glyph(code: Int): String = when (code) {
        0 -> "☀"
        1, 2, 3 -> "⛅"
        in 61..67, in 80..82 -> "☂"
        in 71..77 -> "❄"
        else -> "☁"
    }
}
