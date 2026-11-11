package com.velocita.speedometer.domain.usecase

import com.velocita.speedometer.domain.model.SpeedSafetyZone

/**
 * FASE 1 (3.3): Evaluates current speed (in km/h) against user/preset Yellow and Red thresholds.
 */
class EvaluateThresholdUseCase {
    fun evaluate(
        speedKmh: Float,
        yellowThresholdKmh: Float,
        redThresholdKmh: Float
    ): SpeedSafetyZone {
        val safeYellow = yellowThresholdKmh.coerceAtLeast(1.0f)
        val safeRed = redThresholdKmh.coerceAtLeast(safeYellow + 1.0f)
        return when {
            speedKmh < safeYellow -> SpeedSafetyZone.SAFE_GREEN
            speedKmh < safeRed -> SpeedSafetyZone.WARNING_YELLOW
            else -> SpeedSafetyZone.DANGER_RED
        }
    }
}
