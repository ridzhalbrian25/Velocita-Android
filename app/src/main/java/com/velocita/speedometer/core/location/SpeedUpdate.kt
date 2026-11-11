package com.velocita.speedometer.core.location

/**
 * Section 5.1: Internal Hardware GPS Contract (Client-Side Reactive Flow).
 */
sealed interface SpeedUpdate {
    data class Success(
        val rawSpeedMs: Float,
        val accuracyMeters: Float,
        val timestampEpoch: Long,
        val elapsedRealtimeMs: Long
    ) : SpeedUpdate

    data object GpsDisabled : SpeedUpdate
    data object SignalLost : SpeedUpdate
    data class PermissionMissing(val missingPermission: String) : SpeedUpdate
}
