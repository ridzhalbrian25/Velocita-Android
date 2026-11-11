package com.velocita.speedometer.domain.model

/**
 * FASE 1 (3.2) & FASE 2 (3.5): Supported Speed Units & Conversion Factors from km/h.
 */
enum class SpeedUnit(
    val prefKey: String,
    val displayLabel: String,
    val factorFromKmh: Float
) {
    KMH(prefKey = "KMH", displayLabel = "km/h", factorFromKmh = 1.0f),
    MPH(prefKey = "MPH", displayLabel = "mph", factorFromKmh = 0.621371f),
    KNOT(prefKey = "KNOT", displayLabel = "knots", factorFromKmh = 0.539957f),
    MS(prefKey = "MS", displayLabel = "m/s", factorFromKmh = 0.277778f);

    fun convertFromKmh(speedKmh: Float): Float = speedKmh * factorFromKmh

    companion object {
        fun fromKey(key: String?): SpeedUnit {
            return entries.firstOrNull { it.prefKey.equals(key, ignoreCase = true) } ?: KMH
        }
    }
}
