package com.velocita.speedometer.domain.usecase

/**
 * FASE 3 (3.8): Exponential Moving Average (EMA) Low-Pass Filter for GPS Jitter Reduction.
 *
 * Formula:
 * v_filtered(t) = alpha * v_raw(t) + (1 - alpha) * v_filtered(t - 1)
 *
 * Mitigates multi-path signal reflection inside metallic high-speed train carriages.
 */
class ApplyLowPassFilterUseCase {
    private var previousFilteredSpeedKmh: Float? = null

    fun filter(rawSpeedKmh: Float, alpha: Float): Float {
        val clampedAlpha = alpha.coerceIn(0.01f, 1.0f)
        if (rawSpeedKmh < 0.3f) {
            // Snap to zero when effectively stopped to avoid asymptotic tail
            previousFilteredSpeedKmh = 0.0f
            return 0.0f
        }
        val prev = previousFilteredSpeedKmh
        val filtered = if (prev == null) {
            rawSpeedKmh
        } else {
            (clampedAlpha * rawSpeedKmh) + ((1.0f - clampedAlpha) * prev)
        }
        previousFilteredSpeedKmh = filtered
        return filtered
    }

    fun reset() {
        previousFilteredSpeedKmh = null
    }
}
