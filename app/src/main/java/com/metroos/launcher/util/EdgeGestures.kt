package com.metroos.launcher.util

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * MeeGo-style edge gesture detector for the launcher shell.
 *
 * - Swipe up from the bottom edge   -> [onBottom]  (minimize / go home)
 * - Swipe down from the top edge    -> [onTop]     (quick settings)
 * - Swipe right from the left edge  -> [onLeft]    (previous / open switcher)
 * - Swipe left from the right edge  -> [onRight]   (next / open switcher)
 *
 * The gesture never consumes events, so children (WebView, games, lists)
 * keep full control of their own touches.
 */
fun Modifier.edgeGestures(
    edgeWidth: Dp = 24.dp,
    trigger: Dp = 48.dp,
    onBottom: () -> Unit = {},
    onTop: () -> Unit = {},
    onLeft: () -> Unit = {},
    onRight: () -> Unit = {}
): Modifier = composed {
    val density = LocalDensity.current
    pointerInput(Unit) {
        val edgePx = with(density) { edgeWidth.toPx() }
        val triggerPx = with(density) { trigger.toPx() }
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val start = down.position
            var fired = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                if (change.id != down.id && event.changes.size > 1) continue
                val pos = change.position
                if (!fired) {
                    val dx = pos.x - start.x
                    val dy = pos.y - start.y
                    when {
                        start.y > size.height - edgePx && dy < -triggerPx && abs(dx) < triggerPx -> {
                            onBottom(); fired = true
                        }
                        start.y < edgePx && dy > triggerPx && abs(dx) < triggerPx -> {
                            onTop(); fired = true
                        }
                        start.x < edgePx && dx > triggerPx && abs(dy) < triggerPx -> {
                            onLeft(); fired = true
                        }
                        start.x > size.width - edgePx && dx < -triggerPx && abs(dy) < triggerPx -> {
                            onRight(); fired = true
                        }
                    }
                }
                if (event.changes.all { !it.pressed }) break
            }
        }
    }
}

/** True when [offset] starts within [edgeWidth] of the container edge given [size]. */
fun Offset.nearEdge(widthPx: Float, heightPx: Float, edgeWidth: Dp, density: Float): Boolean {
    val px = edgeWidth.value * density
    return x < px || x > widthPx - px || y < px || y > heightPx - px
}
