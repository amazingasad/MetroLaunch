package com.metroos.launcher.data

import android.content.Intent

data class RecentApp(
    val id: String,
    val label: String,
    val launchIntent: Intent? = null
)

/**
 * Launcher-owned recents list backing the MeeGo task switcher.
 * Apps are registered here when launched from the shell; the switcher
 * cards can be swiped away. Also mirrored to preferences so the
 * gesture-router overlay process view survives launcher eviction.
 */
object RecentsStore {

    private val items = mutableListOf<RecentApp>()
    private val listeners = mutableListOf<() -> Unit>()

    fun all(): List<RecentApp> = items.toList()

    fun push(app: RecentApp) {
        items.removeAll { it.id == app.id }
        items.add(0, app)
        if (items.size > 12) items.removeAt(items.lastIndex)
        notifyChanged()
    }

    fun remove(id: String) {
        items.removeAll { it.id == id }
        notifyChanged()
    }

    fun clear() {
        items.clear()
        notifyChanged()
    }

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyChanged() {
        listeners.forEach { it() }
    }
}
