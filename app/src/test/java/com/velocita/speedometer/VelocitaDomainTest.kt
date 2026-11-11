package com.velocita.speedometer

import com.velocita.speedometer.core.math.HaversineCalculator
import com.velocita.speedometer.domain.model.SpeedSafetyZone
import com.velocita.speedometer.domain.model.SpeedUnit
import com.velocita.speedometer.domain.model.VehicleProfile
import com.velocita.speedometer.domain.usecase.ApplyLowPassFilterUseCase
import com.velocita.speedometer.domain.usecase.EvaluateThresholdUseCase
import com.velocita.speedometer.domain.usecase.SessionMetricsTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VelocitaDomainTest {

    @Test
    fun `Haversine distance and speed fallback calculates accurate velocity`() {
        // ~111.195 meters for 0.001 degree latitude at equator
        val distanceMeters = HaversineCalculator.calculateDistanceMeters(
            lat1Deg = -6.2000,
            lon1Deg = 106.8166,
            lat2Deg = -6.2010,
            lon2Deg = 106.8166
        )
        assertTrue("Expected ~111m but got $distanceMeters", distanceMeters in 110.5..112.0)

        // Over 2 seconds, ~111.195m -> ~55.6 m/s (~200 km/h Whoosh high-speed train)
        val speedMs = HaversineCalculator.calculateSpeedMs(
            lat1Deg = -6.2000,
            lon1Deg = 106.8166,
            time1Ms = 1_000L,
            lat2Deg = -6.2010,
            lon2Deg = 106.8166,
            time2Ms = 3_000L
        )
        assertEquals(55.6f, speedMs, 0.5f)
    }

    @Test
    fun `Low-pass filter smooths spikes according to vehicle alpha`() {
        val filter = ApplyLowPassFilterUseCase()
        val alphaTrain = VehicleProfile.TRAIN.defaultFilterAlpha // 0.15f

        val first = filter.filter(rawSpeedKmh = 100.0f, alpha = alphaTrain)
        assertEquals(100.0f, first, 0.001f)

        // Sudden multipath spike to 200 km/h -> v = 0.15 * 200 + 0.85 * 100 = 115.0 km/h
        val second = filter.filter(rawSpeedKmh = 200.0f, alpha = alphaTrain)
        assertEquals(115.0f, second, 0.01f)
    }

    @Test
    fun `Threshold evaluator maps speeds to WCAG AAA safety zones`() {
        val evaluator = EvaluateThresholdUseCase()
        assertEquals(
            SpeedSafetyZone.SAFE_GREEN,
            evaluator.evaluate(speedKmh = 59.9f, yellowThresholdKmh = 60f, redThresholdKmh = 100f)
        )
        assertEquals(
            SpeedSafetyZone.WARNING_YELLOW,
            evaluator.evaluate(speedKmh = 60.0f, yellowThresholdKmh = 60f, redThresholdKmh = 100f)
        )
        assertEquals(
            SpeedSafetyZone.DANGER_RED,
            evaluator.evaluate(speedKmh = 100.0f, yellowThresholdKmh = 60f, redThresholdKmh = 100f)
        )
    }

    @Test
    fun `SpeedUnit converts accurately to mph and knots`() {
        assertEquals(62.1371f, SpeedUnit.MPH.convertFromKmh(100.0f), 0.001f)
        assertEquals(53.9957f, SpeedUnit.KNOT.convertFromKmh(100.0f), 0.001f)
        assertEquals(27.7778f, SpeedUnit.MS.convertFromKmh(100.0f), 0.01f)
    }

    @Test
    fun `SessionMetricsTracker ignores stationary samples under 2 kmh for moving average`() {
        val tracker = SessionMetricsTracker()
        tracker.onSpeedSample(speedKmh = 0.5f, nowEpochMs = 1000L, nowElapsedRealtimeMs = 1000L)
        val m1 = tracker.onSpeedSample(speedKmh = 1.8f, nowEpochMs = 2000L, nowElapsedRealtimeMs = 2000L)
        assertEquals(0.0f, m1.movingAvgSpeedKmh, 0.001f)
        assertEquals(0L, m1.movingDurationSeconds)

        tracker.onSpeedSample(speedKmh = 80.0f, nowEpochMs = 3000L, nowElapsedRealtimeMs = 3000L)
        val m2 = tracker.onSpeedSample(speedKmh = 120.0f, nowEpochMs = 4000L, nowElapsedRealtimeMs = 4000L)
        assertEquals(120.0f, m2.maxSpeedKmh, 0.001f)
        assertEquals(100.0f, m2.movingAvgSpeedKmh, 0.001f)
        assertEquals(2L, m2.movingDurationSeconds)
    }
}
