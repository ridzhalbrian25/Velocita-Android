package com.velocita.speedometer.domain.model

/**
 * FASE 3 (3.9): Transient In-Memory Real-Time Session Telemetry.
 * Stored strictly in volatile heap memory (Zero-Disk Persistence by default).
 */
data class SessionMetrics(
    val sessionId: String,
    val startTimestampEpochMs: Long,
    val maxSpeedKmh: Float = 0.0f,
    val movingAvgSpeedKmh: Float = 0.0f,
    val totalDistanceMeters: Float = 0.0f,
    val movingDurationSeconds: Long = 0L,
    val sampleCount: Int = 0
)
