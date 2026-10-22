package com.metroos.launcher.util

import android.content.Context
import android.content.SharedPreferences

/** Small synchronous preferences wrapper used by games, tiles and scores. */
object Prefs {
    private const val NAME = "metroos"

    private fun sp(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getString(context: Context, key: String, def: String = ""): String =
        sp(context).getString(key, def) ?: def

    fun putString(context: Context, key: String, value: String) {
        sp(context).edit().putString(key, value).apply()
    }

    fun getInt(context: Context, key: String, def: Int): Int = sp(context).getInt(key, def)

    fun putInt(context: Context, key: String, value: Int) {
        sp(context).edit().putInt(key, value).apply()
    }

    fun remove(context: Context, key: String) {
        sp(context).edit().remove(key).apply()
    }
}
