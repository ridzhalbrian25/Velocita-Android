package com.velocita.speedometer.domain.usecase

import com.velocita.speedometer.domain.model.SessionMetrics
import java.util.UUID
import kotlin.math.max

/**
 * FASE 3 (3.9): Volatile in-memory calculator for session peak speed, moving average
 * (sampled only when v > 2.0 km/h), total distance, and active moving time.
 */
class SessionMetricsTracker {

    companion object {
        const val MOVING_SPEED_THRESHOLD_KMH = 2.0f
    }

    private var sessionId: String = UUID.randomUUID().toString()
    private var startTimestampEpochMs: Long = System.currentTimeMillis()
    private var lastSampleElapsedMs: Long? = null

    private var maxSpeedKmh: Float = 0.0f
    private var cumulativeMovingSpeedKmh: Double = 0.0
    private var movingSampleCount: Int = 0
    private var totalDistanceMeters: Float = 0.0f
    private var totalMovingDurationMs: Long = 0L

    @Synchronized
    fun onSpeedSample(
        speedKmh: Float,
        nowEpochMs: Long,
        nowElapsedRealtimeMs: Long
    ): SessionMetrics {
        if (speedKmh > maxSpeedKmh) {
            maxSpeedKmh = speedKmh
        }

        val prevElapsed = lastSampleElapsedMs
        val deltaMs = if (prevElapsed != null && nowElapsedRealtimeMs > prevElapsed) {
            (nowElapsedRealtimeMs - prevElapsed).coerceAtMost(10_000L)
        } else {
            0L
        }
        lastSampleElapsedMs = nowElapsedRealtimeMs

        if (speedKmh > MOVING_SPEED_THRESHOLD_KMH) {
            cumulativeMovingSpeedKmh += speedKmh.toDouble()
            movingSampleCount += 1
            totalMovingDurationMs += deltaMs
            val speedMs = speedKmh / 3.6f
            totalDistanceMeters += speedMs * (deltaMs / 1000.0f)
        }

        val avgKmh = if (movingSampleCount > 0) {
            (cumulativeMovingSpeedKmh / movingSampleCount).toFloat()
        } else {
            0.0f
        }

        return SessionMetrics(
            sessionId = sessionId,
            startTimestampEpochMs = startTimestampEpochMs,
            maxSpeedKmh = maxSpeedKmh,
            movingAvgSpeedKmh = avgKmh,
            totalDistanceMeters = totalDistanceMeters,
            movingDurationSeconds = max(0L, totalMovingDurationMs / 1000L),
            sampleCount = movingSampleCount
        )
    }

    @Synchronized
    fun reset(nowEpochMs: Long = System.currentTimeMillis()): SessionMetrics {
        sessionId = UUID.randomUUID().toString()
        startTimestampEpochMs = nowEpochMs
        lastSampleElapsedMs = null
        maxSpeedKmh = 0.0f
        cumulativeMovingSpeedKmh = 0.0
        movingSampleCount = 0
        totalDistanceMeters = 0.0f
        totalMovingDurationMs = 0L
        return SessionMetrics(
            sessionId = sessionId,
            startTimestampEpochMs = startTimestampEpochMs
        )
    }
}
