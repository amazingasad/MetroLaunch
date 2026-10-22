package com.metroos.launcher.util

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Shared spring physics presets for the animation engine.
 * Tuned for 60-120 FPS: short settle times, no visible overshoot
 * on structural motion, subtle bounce on playful motion.
 */
object Springs {
    /** Structural moves: tile flips, panel slides. */
    fun fast() = spring<Float>(dampingRatio = 0.9f, stiffness = 420f)

    /** Playful motion: 2048 pops, tile spawn. */
    fun bouncy() = spring<Float>(dampingRatio = 0.55f, stiffness = 320f)

    /** Heavy surfaces: task switcher cards. */
    fun heavy() = spring<Float>(dampingRatio = 1f, stiffness = 210f)

    /** General default. */
    fun standard() = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
}

/** Velocity-derived damping: fast flings feel loose, slow drags feel tight. */
fun velocityDamping(velocity: Float): Float =
    when {
        velocity > 4000f -> 0.6f
        velocity > 1500f -> 0.75f
        else -> 1f
    }
