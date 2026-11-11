package com.velocita.speedometer.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.velocita.speedometer.domain.model.SpeedUnit
import com.velocita.speedometer.domain.model.VehicleProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

/**
 * Section 4.1: Exact DataStore Preferences Schema (Key-Value)
 * - pref_speed_unit: String ("KMH", "MPH", "KNOT", "MS") default "KMH"
 * - pref_vehicle_profile: String ("BUS", "TRAIN", "BIKE", "CUSTOM") default "TRAIN"
 * - pref_threshold_yellow: Float default 60.0f
 * - pref_threshold_red: Float default 100.0f
 * - pref_keep_screen_on: Boolean default true
 * - pref_hud_mirrored: Boolean default false
 * - pref_smoothing_factor: Float default 0.25f
 */
data class UserPreferences(
    val speedUnit: SpeedUnit = SpeedUnit.KMH,
    val vehicleProfile: VehicleProfile = VehicleProfile.TRAIN,
    val thresholdYellowKmh: Float = 60.0f,
    val thresholdRedKmh: Float = 100.0f,
    val keepScreenOn: Boolean = true,
    val hudMirrored: Boolean = false,
    val smoothingFactor: Float = 0.25f
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val SPEED_UNIT = stringPreferencesKey("pref_speed_unit")
        val VEHICLE_PROFILE = stringPreferencesKey("pref_vehicle_profile")
        val THRESHOLD_YELLOW = floatPreferencesKey("pref_threshold_yellow")
        val THRESHOLD_RED = floatPreferencesKey("pref_threshold_red")
        val KEEP_SCREEN_ON = booleanPreferencesKey("pref_keep_screen_on")
        val HUD_MIRRORED = booleanPreferencesKey("pref_hud_mirrored")
        val SMOOTHING_FACTOR = floatPreferencesKey("pref_smoothing_factor")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val profile = VehicleProfile.fromId(prefs[Keys.VEHICLE_PROFILE] ?: "TRAIN")
            UserPreferences(
                speedUnit = SpeedUnit.fromKey(prefs[Keys.SPEED_UNIT] ?: "KMH"),
                vehicleProfile = profile,
                thresholdYellowKmh = prefs[Keys.THRESHOLD_YELLOW] ?: 60.0f,
                thresholdRedKmh = prefs[Keys.THRESHOLD_RED] ?: 100.0f,
                keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
                hudMirrored = prefs[Keys.HUD_MIRRORED] ?: false,
                smoothingFactor = (prefs[Keys.SMOOTHING_FACTOR] ?: 0.25f).coerceIn(0.01f, 1.0f)
            )
        }

    suspend fun setSpeedUnit(unit: SpeedUnit) {
        context.dataStore.edit { it[Keys.SPEED_UNIT] = unit.prefKey }
    }

    suspend fun setVehicleProfile(profile: VehicleProfile) {
        context.dataStore.edit { prefs ->
            prefs[Keys.VEHICLE_PROFILE] = profile.id
            prefs[Keys.THRESHOLD_YELLOW] = profile.defaultYellowThresholdKmh
            prefs[Keys.THRESHOLD_RED] = profile.defaultRedThresholdKmh
            prefs[Keys.SMOOTHING_FACTOR] = profile.defaultFilterAlpha
        }
    }

    suspend fun setCustomThresholds(yellowKmh: Float, redKmh: Float) {
        val validYellow = yellowKmh.coerceIn(5.0f, 350.0f)
        val validRed = redKmh.coerceIn(validYellow + 5.0f, 400.0f)
        context.dataStore.edit { prefs ->
            prefs[Keys.VEHICLE_PROFILE] = VehicleProfile.CUSTOM.id
            prefs[Keys.THRESHOLD_YELLOW] = validYellow
            prefs[Keys.THRESHOLD_RED] = validRed
        }
    }

    suspend fun setSmoothingFactor(alpha: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SMOOTHING_FACTOR] = alpha.coerceIn(0.01f, 1.0f)
        }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }

    suspend fun setHudMirrored(mirrored: Boolean) {
        context.dataStore.edit { it[Keys.HUD_MIRRORED] = mirrored }
    }
}
