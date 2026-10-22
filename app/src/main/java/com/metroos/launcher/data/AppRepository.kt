package com.metroos.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val packageName: String,
    val launchIntent: Intent,
    val category: String,
    val icon: Drawable? = null
)

/**
 * Loads every launchable third-party app installed on the device and
 * assigns a coarse category for the drawer's auto-categorization.
 */
object AppRepository {

    fun loadApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = try {
            pm.queryIntentActivities(query, 0)
        } catch (_: Exception) {
            emptyList()
        }
        return resolved.mapNotNull { info ->
            val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
            if (pkg == context.packageName) return@mapNotNull null
            val label = try {
                info.loadLabel(pm).toString()
            } catch (_: Exception) {
                pkg
            }
            val launch = pm.getLaunchIntentForPackage(pkg) ?: return@mapNotNull null
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            AppInfo(
                label = label,
                packageName = pkg,
                launchIntent = launch,
                category = guessCategory(label, pkg),
                icon = try {
                    info.loadIcon(pm)
                } catch (_: Exception) {
                    null
                }
            )
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }

    /** Very light heuristic categorization for the drawer sections. */
    fun guessCategory(label: String, pkg: String): String {
        val s = (pkg + " " + label).lowercase()
        return when {
            s.contains("game") || s.contains("chess") || s.contains("2048") -> "Games"
            s.contains("social") || s.contains("chat") || s.contains("telegram") ||
                s.contains("whatsapp") || s.contains("instagram") || s.contains("x.com") -> "Social"
            s.contains("music") || s.contains("spotify") || s.contains("podcast") ||
                s.contains("video") || s.contains("tube") || s.contains("netflix") -> "Media"
            s.contains("mail") || s.contains("gmail") || s.contains("outlook") ||
                s.contains("calendar") -> "Work"
            s.contains("camera") || s.contains("photo") || s.contains("gallery") -> "Photos"
            else -> "Other"
        }
    }
}
