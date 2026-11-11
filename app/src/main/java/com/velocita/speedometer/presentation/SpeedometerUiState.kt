package com.velocita.speedometer.presentation

import com.velocita.speedometer.domain.model.SessionMetrics
import com.velocita.speedometer.domain.model.SpeedSafetyZone
import com.velocita.speedometer.domain.model.SpeedUnit
import com.velocita.speedometer.domain.model.VehicleProfile

/**
 * Section 2.2 & 3.1: Deterministic MVI ViewState for Unidirectional Data Flow (UDF).
 */
sealed interface SpeedometerScreenStatus {
    data object PermissionDenied : SpeedometerScreenStatus
    data object Active : SpeedometerScreenStatus
}

data class SpeedometerUiState(
    val screenStatus: SpeedometerScreenStatus = SpeedometerScreenStatus.Active,
    val rawSpeedKmh: Float = 0.0f,
    val filteredSpeedKmh: Float = 0.0f,
    val displayedSpeed: Float = 0.0f,
    val speedUnit: SpeedUnit = SpeedUnit.KMH,
    val vehicleProfile: VehicleProfile = VehicleProfile.TRAIN,
    val safetyZone: SpeedSafetyZone = SpeedSafetyZone.SAFE_GREEN,
    val thresholdYellowKmh: Float = 60.0f,
    val thresholdRedKmh: Float = 100.0f,
    val smoothingAlpha: Float = 0.25f,
    val accuracyMeters: Float = 0.0f,
    val isGpsDisabled: Boolean = false,
    val isWeakSignal: Boolean = false,
    val isHudMirrored: Boolean = false,
    val keepScreenOn: Boolean = true,
    val screenBrightness: Float = 0.85f,
    val isBatterySaverTriggered: Boolean = false,
    val batteryPercentage: Int = 100,
    val sessionMetrics: SessionMetrics = SessionMetrics(
        sessionId = "initial",
        startTimestampEpochMs = 0L
    )
)
