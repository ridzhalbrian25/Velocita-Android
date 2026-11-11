package com.velocita.speedometer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Section 8.4 + Full FASE 1, 2, 3 Enhancements:
 * - True OLED Black (#000000) background
 * - Giant Tabular Numbers (`fontFeatureSettings = "tnum"`) occupying >=65% height in landscape and >=45% in portrait
 * - WCAG AAA dynamic color transition (250ms FastOutSlowInEasing)
 * - Weak signal alpha dimming (0.35f) & static satellite lost cue
 * - HUD horizontal mirroring (`graphicsLayer { scaleX = if (isHudMode) -1f else 1f }`)
 * - Double-tap center reset with micro haptic feedback (`HapticFeedbackType.LongPress`)
 * - Right-edge vertical swipe gesture for brightness control (0.01f .. 1.0f)
 */
@Composable
fun SpeedDisplay(
    speed: Float,
    unitLabel: String,
    accuracy: Float,
    yellowThresholdDisplay: Float,
    redThresholdDisplay: Float,
    isHudMode: Boolean,
    isWeakSignal: Boolean,
    isBatterySaverActive: Boolean,
    maxSpeedDisplay: Float,
    avgSpeedDisplay: Float,
    movingDurationSeconds: Long,
    onDoubleTapReset: () -> Unit,
    onBrightnessDragDelta: (Float) -> Unit,
    pixelShiftModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Evaluate color based on WCAG AAA contrast threshold against #000000
    val targetColor = when {
        speed < yellowThresholdDisplay -> Color(0xFF00E676) // Safe Green (13.9:1)
        speed < redThresholdDisplay -> Color(0xFFFFEA00)    // Neon Yellow (17.1:1)
        else -> Color(0xFFFF1744)                           // Vivid Red (5.8:1 Large Bold)
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "SpeedColorAnimation"
    )

    val digitAlpha = if (isWeakSignal) 0.35f else 1.0f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000)) // Strictly hardcoded OLED True Black
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDoubleTapReset()
                    }
                )
            }
            .graphicsLayer {
                // FASE 3 (3.7): HUD Horizontal Mirroring
                scaleX = if (isHudMode) -1f else 1f
            }
    ) {
        val isLandscape = maxWidth > maxHeight
        val density = LocalDensity.current

        // FASE 1 (3.2): Occupy >= 65% vertical height in landscape, >= 45% in portrait
        val targetDigitHeightDp = if (isLandscape) maxHeight * 0.65f else maxHeight * 0.45f
        val dynamicFontSizeSp = with(density) {
            (targetDigitHeightDp.toPx() * 0.78f).toSp()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(pixelShiftModifier),
            contentAlignment = Alignment.Center
        ) {
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Telemetry Column (Accuracy + Transient Session Metrics)
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.weight(0.25f)
                    ) {
                        AccuracyHeader(
                            accuracy = accuracy,
                            isWeakSignal = isWeakSignal,
                            isBatterySaverActive = isBatterySaverActive
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TransientMetricsBar(
                            maxSpeed = maxSpeedDisplay,
                            avgSpeed = avgSpeedDisplay,
                            unitLabel = unitLabel,
                            movingDurationSeconds = movingDurationSeconds,
                            isCompactColumn = true
                        )
                    }

                    // Center Giant Tabular Speed Digits
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(0.60f)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.0f", speed),
                            color = animatedColor,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = dynamicFontSizeSp,
                                lineHeight = dynamicFontSizeSp,
                                fontFeatureSettings = "tnum",
                                textAlign = TextAlign.Center
                            ),
                            maxLines = 1,
                            modifier = Modifier.alpha(digitAlpha)
                        )
                    }

                    // Right Unit Indicator Column
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(0.15f)
                    ) {
                        Text(
                            text = unitLabel.lowercase(Locale.US),
                            color = Color(0xFF888888),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                        if (isHudMode) {
                            Text(
                                text = "HUD",
                                color = Color(0xFF00E676),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                // Portrait Orientation Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    AccuracyHeader(
                        accuracy = accuracy,
                        isWeakSignal = isWeakSignal,
                        isBatterySaverActive = isBatterySaverActive
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.0f", speed),
                            color = animatedColor,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = dynamicFontSizeSp,
                                lineHeight = dynamicFontSizeSp,
                                fontFeatureSettings = "tnum",
                                textAlign = TextAlign.Center
                            ),
                            maxLines = 1,
                            modifier = Modifier.alpha(digitAlpha)
                        )

                        Text(
                            text = unitLabel.lowercase(Locale.US),
                            color = Color(0xFF888888),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    TransientMetricsBar(
                        maxSpeed = maxSpeedDisplay,
                        avgSpeed = avgSpeedDisplay,
                        unitLabel = unitLabel,
                        movingDurationSeconds = movingDurationSeconds,
                        isCompactColumn = false
                    )
                }
            }

            // FASE 3 (3.7): Right-edge touch strip for single-finger vertical brightness control
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(56.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            val heightPx = size.height.toFloat().coerceAtLeast(1f)
                            // Dragging up (negative dragAmount) increases brightness
                            val deltaNormalized = (-dragAmount / heightPx) * 1.2f
                            onBrightnessDragDelta(deltaNormalized)
                        }
                    }
            )
        }
    }
}

@Composable
private fun AccuracyHeader(
    accuracy: Float,
    isWeakSignal: Boolean,
    isBatterySaverActive: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!isBatterySaverActive) {
            Text(
                text = "ACC: ±${accuracy.toInt()}m",
                color = Color(0xFF666666),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.Monospace
            )
        } else {
            Text(
                text = "ECO <15%",
                color = Color(0xFFFFEA00),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        if (isWeakSignal) {
            // FASE 2 (3.6): Minimalist static weak/lost satellite cue without distracting animations
            Text(
                text = "[SAT LOST]",
                color = Color(0xFFFF1744),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun TransientMetricsBar(
    maxSpeed: Float,
    avgSpeed: Float,
    unitLabel: String,
    movingDurationSeconds: Long,
    isCompactColumn: Boolean
) {
    val minutes = movingDurationSeconds / 60L
    val seconds = movingDurationSeconds % 60L
    val timeText = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    if (isCompactColumn) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MetricItem(label = "MAX", value = String.format(Locale.US, "%.1f %s", maxSpeed, unitLabel))
            MetricItem(label = "AVG", value = String.format(Locale.US, "%.1f %s", avgSpeed, unitLabel))
            MetricItem(label = "MOV", value = timeText)
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricItem(label = "MAX", value = String.format(Locale.US, "%.1f", maxSpeed))
            MetricItem(label = "AVG", value = String.format(Locale.US, "%.1f", avgSpeed))
            MetricItem(label = "TIME", value = timeText)
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = Color(0xFF555555),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = Color(0xFF999999),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            style = TextStyle(fontFeatureSettings = "tnum")
        )
    }
}
