package com.velocita.speedometer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.velocita.speedometer.core.location.AndroidLocationEngine
import com.velocita.speedometer.core.location.SpeedUpdate
import com.velocita.speedometer.core.power.BatterySaverMonitor
import com.velocita.speedometer.data.preferences.UserPreferencesRepository
import com.velocita.speedometer.domain.model.SpeedUnit
import com.velocita.speedometer.domain.model.VehicleProfile
import com.velocita.speedometer.domain.usecase.ApplyLowPassFilterUseCase
import com.velocita.speedometer.domain.usecase.EvaluateThresholdUseCase
import com.velocita.speedometer.domain.usecase.SessionMetricsTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Section 2.2: Presentation Layer ViewModel coordinating Location Flow, Preferences DataStore,
 * EMA Low-Pass Filter, WCAG AAA Threshold Evaluator, Session Metrics, and Battery Saver.
 */
class SpeedometerViewModel(
    private val locationEngine: AndroidLocationEngine,
    private val preferencesRepository: UserPreferencesRepository,
    private val batterySaverMonitor: BatterySaverMonitor,
    private val lowPassFilterUseCase: ApplyLowPassFilterUseCase = ApplyLowPassFilterUseCase(),
    private val evaluateThresholdUseCase: EvaluateThresholdUseCase = EvaluateThresholdUseCase(),
    private val sessionMetricsTracker: SessionMetricsTracker = SessionMetricsTracker()
) : ViewModel() {

    companion object {
        const val WEAK_ACCURACY_THRESHOLD_METERS = 25.0f
    }

    private val _uiState = MutableStateFlow(SpeedometerUiState())
    val uiState: StateFlow<SpeedometerUiState> = _uiState.asStateFlow()

    private var locationObservationJob: Job? = null

    init {
        observePreferences()
        observeBatterySaver()
        checkPermissionAndStartTracking()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.preferencesFlow.collect { prefs ->
                _uiState.update { current ->
                    val convertedDisplaySpeed = prefs.speedUnit.convertFromKmh(current.filteredSpeedKmh)
                    val zone = evaluateThresholdUseCase.evaluate(
                        speedKmh = current.filteredSpeedKmh,
                        yellowThresholdKmh = prefs.thresholdYellowKmh,
                        redThresholdKmh = prefs.thresholdRedKmh
                    )
                    current.copy(
                        speedUnit = prefs.speedUnit,
                        vehicleProfile = prefs.vehicleProfile,
                        thresholdYellowKmh = prefs.thresholdYellowKmh,
                        thresholdRedKmh = prefs.thresholdRedKmh,
                        smoothingAlpha = prefs.smoothingFactor,
                        keepScreenOn = prefs.keepScreenOn,
                        isHudMirrored = prefs.hudMirrored,
                        displayedSpeed = convertedDisplaySpeed,
                        safetyZone = zone
                    )
                }
            }
        }
    }

    private fun observeBatterySaver() {
        viewModelScope.launch {
            batterySaverMonitor.observeBatteryState().collect { battery ->
                _uiState.update { current ->
                    val enforcedBrightness = if (battery.isLowBatterySaverActive) {
                        BatterySaverMonitor.BATTERY_SAVER_BRIGHTNESS
                    } else {
                        current.screenBrightness
                    }
                    current.copy(
                        batteryPercentage = battery.batteryPct,
                        isBatterySaverTriggered = battery.isLowBatterySaverActive,
                        screenBrightness = enforcedBrightness
                    )
                }
            }
        }
    }

    fun checkPermissionAndStartTracking() {
        if (!locationEngine.hasFineLocationPermission()) {
            _uiState.update {
                it.copy(screenStatus = SpeedometerScreenStatus.PermissionDenied)
            }
            return
        }

        _uiState.update {
            it.copy(
                screenStatus = SpeedometerScreenStatus.Active,
                isGpsDisabled = !locationEngine.isGpsHardwareEnabled()
            )
        }

        locationObservationJob?.cancel()
        locationObservationJob = viewModelScope.launch {
            locationEngine.observeSpeed(adaptivePollingEnabled = true).collect { update ->
                when (update) {
                    is SpeedUpdate.PermissionMissing -> {
                        _uiState.update {
                            it.copy(screenStatus = SpeedometerScreenStatus.PermissionDenied)
                        }
                    }

                    is SpeedUpdate.GpsDisabled -> {
                        _uiState.update {
                            it.copy(
                                isGpsDisabled = true,
                                isWeakSignal = true,
                                displayedSpeed = 0.0f
                            )
                        }
                    }

                    is SpeedUpdate.SignalLost -> {
                        _uiState.update {
                            it.copy(isWeakSignal = true)
                        }
                    }

                    is SpeedUpdate.Success -> {
                        val stateSnapshot = _uiState.value
                        val rawSpeedKmh = update.rawSpeedMs * 3.6f
                        val filteredKmh = lowPassFilterUseCase.filter(
                            rawSpeedKmh = rawSpeedKmh,
                            alpha = stateSnapshot.smoothingAlpha
                        )
                        val displaySpeed = stateSnapshot.speedUnit.convertFromKmh(filteredKmh)
                        val safetyZone = evaluateThresholdUseCase.evaluate(
                            speedKmh = filteredKmh,
                            yellowThresholdKmh = stateSnapshot.thresholdYellowKmh,
                            redThresholdKmh = stateSnapshot.thresholdRedKmh
                        )

                        // FASE 4 (3.11): Skip secondary accuracy processing if battery saver <15% is active
                        val weakSignal = if (stateSnapshot.isBatterySaverTriggered) {
                            false
                        } else {
                            update.accuracyMeters > WEAK_ACCURACY_THRESHOLD_METERS
                        }

                        val updatedMetrics = sessionMetricsTracker.onSpeedSample(
                            speedKmh = filteredKmh,
                            nowEpochMs = update.timestampEpoch,
                            nowElapsedRealtimeMs = update.elapsedRealtimeMs
                        )

                        _uiState.update { current ->
                            current.copy(
                                screenStatus = SpeedometerScreenStatus.Active,
                                rawSpeedKmh = rawSpeedKmh,
                                filteredSpeedKmh = filteredKmh,
                                displayedSpeed = displaySpeed,
                                safetyZone = safetyZone,
                                accuracyMeters = if (current.isBatterySaverTriggered) 0f else update.accuracyMeters,
                                isGpsDisabled = false,
                                isWeakSignal = weakSignal,
                                sessionMetrics = updatedMetrics
                            )
                        }
                    }
                }
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            checkPermissionAndStartTracking()
        } else {
            _uiState.update {
                it.copy(screenStatus = SpeedometerScreenStatus.PermissionDenied)
            }
        }
    }

    fun selectSpeedUnit(unit: SpeedUnit) {
        viewModelScope.launch {
            preferencesRepository.setSpeedUnit(unit)
        }
    }

    fun selectVehicleProfile(profile: VehicleProfile) {
        lowPassFilterUseCase.reset()
        viewModelScope.launch {
            preferencesRepository.setVehicleProfile(profile)
        }
    }

    fun updateCustomThresholds(yellowKmh: Float, redKmh: Float) {
        viewModelScope.launch {
            preferencesRepository.setCustomThresholds(yellowKmh, redKmh)
        }
    }

    fun updateSmoothingFactor(alpha: Float) {
        viewModelScope.launch {
            preferencesRepository.setSmoothingFactor(alpha)
        }
    }

    fun toggleHudMode() {
        val next = !_uiState.value.isHudMirrored
        viewModelScope.launch {
            preferencesRepository.setHudMirrored(next)
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setKeepScreenOn(enabled)
        }
    }

    fun adjustBrightnessByDelta(deltaNormalized: Float) {
        _uiState.update { current ->
            val nextBrightness = (current.screenBrightness + deltaNormalized).coerceIn(0.01f, 1.0f)
            current.copy(screenBrightness = nextBrightness)
        }
    }

    fun resetSessionMetrics() {
        lowPassFilterUseCase.reset()
        val resetMetrics = sessionMetricsTracker.reset()
        _uiState.update { current ->
            current.copy(sessionMetrics = resetMetrics)
        }
    }

    class Factory(
        private val locationEngine: AndroidLocationEngine,
        private val preferencesRepository: UserPreferencesRepository,
        private val batterySaverMonitor: BatterySaverMonitor
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SpeedometerViewModel(
                locationEngine = locationEngine,
                preferencesRepository = preferencesRepository,
                batterySaverMonitor = batterySaverMonitor
            ) as T
        }
    }
}
