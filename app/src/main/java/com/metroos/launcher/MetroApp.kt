package com.metroos.launcher

import android.app.Application
import com.metroos.launcher.data.TileStore

class MetroApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TileStore.init(this)
    }
}
