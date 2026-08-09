package com.hdclark.simbikepark

import kotlin.math.PI
import kotlin.math.sin

/**
 * Screen-space rider presentation rules.
 *
 * Trail direction is a yaw in the isometric world and must not be applied as roll
 * to the side-view rider emoji. Normal feature motion is converted to a bounded
 * lean so successful riders remain upright. Crash/explicit-flip animations bypass
 * this helper and are allowed to rotate freely.
 */
object RiderVisuals {
    const val MAX_NORMAL_LEAN_DEGREES = 55f

    fun uprightLean(requestedDegrees: Float): Float {
        val radians = requestedDegrees * PI.toFloat() / 180f
        return (sin(radians.toDouble()).toFloat() * MAX_NORMAL_LEAN_DEGREES)
            .coerceIn(-MAX_NORMAL_LEAN_DEGREES, MAX_NORMAL_LEAN_DEGREES)
    }
}
