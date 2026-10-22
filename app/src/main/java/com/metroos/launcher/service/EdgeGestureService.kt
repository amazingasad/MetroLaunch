package com.metroos.launcher.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import kotlin.math.abs

/**
 * System-wide MeeGo edge-swipe compositor.
 *
 * Observes raw motion events and detects edge swipes even while a
 * third-party app is in the foreground. When a gesture fires, it starts
 * [GestureRouterActivity] which renders the matching OS surface over
 * whatever is on screen.
 *
 * Motion-event observation requires Android 14 (API 34)+ where the
 * service declares motionEventSources. On older devices the in-app edge
 * gestures (shell modifier) still cover everything inside the launcher.
 */
class EdgeGestureService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.DEFAULT
        }
        if (Build.VERSION.SDK_INT >= 34) {
            info.motionEventSources = android.view.InputDevice.SOURCE_TOUCHSCREEN
        }
        serviceInfo = info
    }

    override fun onMotionEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                fired = false
                screenW = resources.displayMetrics.widthPixels
                screenH = resources.displayMetrics.heightPixels
            }
            MotionEvent.ACTION_MOVE -> {
                if (fired) return
                val dx = event.rawX - startX
                val dy = event.rawY - startY
                val edge = edgePx
                val trigger = triggerPx
                val mode = when {
                    startY > screenH - edge && dy < -trigger && abs(dx) < trigger ->
                        GestureRouterActivity.ACTION_HOME
                    startY < edge && dy > trigger && abs(dx) < trigger ->
                        GestureRouterActivity.ACTION_QUICK_SETTINGS
                    startX < edge && dx > trigger && abs(dy) < trigger ->
                        GestureRouterActivity.ACTION_SWITCHER
                    startX > screenW - edge && dx < -trigger && abs(dy) < trigger ->
                        GestureRouterActivity.ACTION_SWITCHER
                    else -> null
                }
                if (mode != null) {
                    fired = true
                    routeTo(mode)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                startX = -1f; startY = -1f
            }
        }
    }

    private fun routeTo(mode: String) {
        runCatching {
            startActivity(
                Intent(this, GestureRouterActivity::class.java)
                    .setAction(mode)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    )
            )
        }.onFailure { Log.w(TAG, "router launch failed", it) }
    }

    private val edgePx: Float get() = 24f * resources.displayMetrics.density
    private val triggerPx: Float get() = 48f * resources.displayMetrics.density

    private var startX = -1f
    private var startY = -1f
    private var screenW = 0
    private var screenH = 0
    private var fired = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    companion object {
        private const val TAG = "EdgeGestureService"
    }
}
