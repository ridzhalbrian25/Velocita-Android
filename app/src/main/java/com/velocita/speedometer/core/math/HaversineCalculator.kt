package com.velocita.speedometer.core.math

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * FASE 1 (3.1): Pure mathematical Haversine fallback calculator.
 * Used when Location.hasSpeed() is false or returns a negative value.
 */
object HaversineCalculator {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /**
     * Computes great-circle distance in meters between two WGS84 coordinates.
     *
     * Formula:
     * d = 2R * arcsin(sqrt(sin^2(dPhi/2) + cos(phi1)*cos(phi2)*sin^2(dLambda/2)))
     */
    fun calculateDistanceMeters(
        lat1Deg: Double,
        lon1Deg: Double,
        lat2Deg: Double,
        lon2Deg: Double
    ): Double {
        val phi1 = Math.toRadians(lat1Deg)
        val phi2 = Math.toRadians(lat2Deg)
        val deltaPhi = Math.toRadians(lat2Deg - lat1Deg)
        val deltaLambda = Math.toRadians(lon2Deg - lon1Deg)

        val sinHalfDeltaPhi = sin(deltaPhi / 2.0)
        val sinHalfDeltaLambda = sin(deltaLambda / 2.0)

        val a = (sinHalfDeltaPhi * sinHalfDeltaPhi) +
            (cos(phi1) * cos(phi2) * sinHalfDeltaLambda * sinHalfDeltaLambda)
        val clampedA = a.coerceIn(0.0, 1.0)

        return 2.0 * EARTH_RADIUS_METERS * asin(sqrt(clampedA))
    }

    /**
     * Calculates scalar speed in meters per second (m/s) between two samples.
     */
    fun calculateSpeedMs(
        lat1Deg: Double,
        lon1Deg: Double,
        time1Ms: Long,
        lat2Deg: Double,
        lon2Deg: Double,
        time2Ms: Long
    ): Float {
        val deltaTimeMs = time2Ms - time1Ms
        if (deltaTimeMs <= 0L) return 0f
        val distanceMeters = calculateDistanceMeters(lat1Deg, lon1Deg, lat2Deg, lon2Deg)
        val deltaTimeSeconds = deltaTimeMs / 1000.0
        val speedMs = (distanceMeters / deltaTimeSeconds).toFloat()
        // Reject impossible GPS teleportation spikes (> 400 m/s)
        return if (speedMs.isFinite() && speedMs in 0f..400f) speedMs else 0f
    }
}
