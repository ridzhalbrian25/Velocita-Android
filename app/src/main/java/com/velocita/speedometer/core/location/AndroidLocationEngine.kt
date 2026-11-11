package com.velocita.speedometer.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.velocita.speedometer.core.math.HaversineCalculator
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * FASE 1 (3.1), FASE 2 (3.6), & FASE 4 (3.11):
 * Pure Android LocationManager hardware GPS engine.
 * - Zero Google Play Services dependency (< 3MB binary target)
 * - Haversine fallback when Location.hasSpeed() == false
 * - Sensory Sandboxing (NFR 6.1): Lat/Lon coordinates are immediately discarded after scalar speed extraction
 * - Weak signal watchdog (> 4000ms without fix emits SignalLost)
 * - Adaptive Polling Frequency (FASE 4): Switches from 1000ms to 4000ms when stationary (< 1.0 km/h for > 30s)
 */
class AndroidLocationEngine(private val context: Context) {

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    companion object {
        const val NORMAL_POLLING_INTERVAL_MS = 1000L
        const val STATIONARY_POLLING_INTERVAL_MS = 4000L
        const val STATIONARY_THRESHOLD_KMH = 1.0f
        const val RESUME_MOVING_THRESHOLD_KMH = 2.0f
        const val STATIONARY_TRIGGER_DURATION_MS = 30_000L
        const val SIGNAL_LOST_TIMEOUT_MS = 4_000L
    }

    fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isGpsHardwareEnabled(): Boolean {
        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun observeSpeed(adaptivePollingEnabled: Boolean = true): Flow<SpeedUpdate> = callbackFlow {
        if (!hasFineLocationPermission()) {
            trySend(SpeedUpdate.PermissionMissing(Manifest.permission.ACCESS_FINE_LOCATION))
            close()
            return@callbackFlow
        }

        if (!isGpsHardwareEnabled()) {
            trySend(SpeedUpdate.GpsDisabled)
        }

        // Transient primitive holders for Haversine fallback only (Sensory Sandboxing NFR 6.1)
        var prevLat = Double.NaN
        var prevLon = Double.NaN
        var prevFixTimeMs = 0L

        var lastFixElapsedRealtimeMs = SystemClock.elapsedRealtime()
        var stationarySinceElapsedMs: Long? = null
        var currentPollingIntervalMs = NORMAL_POLLING_INTERVAL_MS

        lateinit var locationListener: LocationListener

        fun registerGpsUpdates(intervalMs: Long) {
            try {
                locationManager.removeUpdates(locationListener)
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    intervalMs,
                    0.0f,
                    locationListener,
                    Looper.getMainLooper()
                )
                currentPollingIntervalMs = intervalMs
            } catch (_: SecurityException) {
                trySend(SpeedUpdate.PermissionMissing(Manifest.permission.ACCESS_FINE_LOCATION))
            } catch (_: Exception) {
                trySend(SpeedUpdate.GpsDisabled)
            }
        }

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val nowElapsedMs = SystemClock.elapsedRealtime()
                lastFixElapsedRealtimeMs = nowElapsedMs

                // Extract scalar speed from GPS silicon or fallback to Haversine
                val currentLat = location.latitude
                val currentLon = location.longitude
                val currentTimeMs = location.time

                val rawSpeedMs: Float = if (location.hasSpeed() && location.speed >= 0.0f) {
                    location.speed
                } else if (!prevLat.isNaN() && !prevLon.isNaN() && prevFixTimeMs > 0L) {
                    HaversineCalculator.calculateSpeedMs(
                        lat1Deg = prevLat,
                        lon1Deg = prevLon,
                        time1Ms = prevFixTimeMs,
                        lat2Deg = currentLat,
                        lon2Deg = currentLon,
                        time2Ms = currentTimeMs
                    )
                } else {
                    0.0f
                }

                // Update transient primitives and let Location reference go out of scope (NFR 6.1)
                prevLat = currentLat
                prevLon = currentLon
                prevFixTimeMs = currentTimeMs

                val accuracyMeters = if (location.hasAccuracy()) location.accuracy else 99.0f
                val speedKmh = rawSpeedMs * 3.6f

                // FASE 4 (3.11): Adaptive Polling Frequency Engine
                if (adaptivePollingEnabled) {
                    if (speedKmh < STATIONARY_THRESHOLD_KMH) {
                        val startedStationary = stationarySinceElapsedMs ?: nowElapsedMs.also {
                            stationarySinceElapsedMs = it
                        }
                        if (nowElapsedMs - startedStationary > STATIONARY_TRIGGER_DURATION_MS &&
                            currentPollingIntervalMs != STATIONARY_POLLING_INTERVAL_MS
                        ) {
                            registerGpsUpdates(STATIONARY_POLLING_INTERVAL_MS)
                        }
                    } else if (speedKmh >= RESUME_MOVING_THRESHOLD_KMH) {
                        stationarySinceElapsedMs = null
                        if (currentPollingIntervalMs != NORMAL_POLLING_INTERVAL_MS) {
                            registerGpsUpdates(NORMAL_POLLING_INTERVAL_MS)
                        }
                    }
                }

                trySend(
                    SpeedUpdate.Success(
                        rawSpeedMs = rawSpeedMs,
                        accuracyMeters = accuracyMeters,
                        timestampEpoch = currentTimeMs,
                        elapsedRealtimeMs = nowElapsedMs
                    )
                )
            }

            override fun onProviderEnabled(provider: String) {
                if (provider == LocationManager.GPS_PROVIDER) {
                    lastFixElapsedRealtimeMs = SystemClock.elapsedRealtime()
                }
            }

            override fun onProviderDisabled(provider: String) {
                if (provider == LocationManager.GPS_PROVIDER) {
                    trySend(SpeedUpdate.GpsDisabled)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                // Retained for Android API 26-28 backward compatibility
            }
        }

        // BroadcastReceiver for LocationManager.PROVIDERS_CHANGED_ACTION (FASE 1 - 3.1)
        val providerReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                    if (!isGpsHardwareEnabled()) {
                        trySend(SpeedUpdate.GpsDisabled)
                    } else {
                        registerGpsUpdates(NORMAL_POLLING_INTERVAL_MS)
                    }
                }
            }
        }

        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(providerReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(providerReceiver, filter)
        }

        registerGpsUpdates(NORMAL_POLLING_INTERVAL_MS)

        // FASE 2 (3.6): Watchdog coroutine for tunnel / blindspot signal loss (> 4 seconds)
        val watchdogJob = launch {
            while (isActive) {
                delay(1000L)
                if (isGpsHardwareEnabled()) {
                    val elapsedSinceFix = SystemClock.elapsedRealtime() - lastFixElapsedRealtimeMs
                    val timeoutThreshold = if (currentPollingIntervalMs == STATIONARY_POLLING_INTERVAL_MS) {
                        STATIONARY_POLLING_INTERVAL_MS + 2000L
                    } else {
                        SIGNAL_LOST_TIMEOUT_MS
                    }
                    if (elapsedSinceFix > timeoutThreshold) {
                        trySend(SpeedUpdate.SignalLost)
                    }
                }
            }
        }

        awaitClose {
            watchdogJob.cancel()
            try {
                context.unregisterReceiver(providerReceiver)
            } catch (_: Exception) {
            }
            try {
                locationManager.removeUpdates(locationListener)
            } catch (_: Exception) {
            }
            // Clear transient coordinates from memory
            prevLat = Double.NaN
            prevLon = Double.NaN
            prevFixTimeMs = 0L
        }
    }
}
