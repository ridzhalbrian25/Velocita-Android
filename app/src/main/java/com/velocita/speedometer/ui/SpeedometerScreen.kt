package com.velocita.speedometer.ui

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.velocita.speedometer.R
import com.velocita.speedometer.presentation.SpeedometerScreenStatus
import com.velocita.speedometer.presentation.SpeedometerUiState
import com.velocita.speedometer.presentation.SpeedometerViewModel
import com.velocita.speedometer.ui.components.HardwareWindowControllerEffect
import com.velocita.speedometer.ui.components.SettingsBottomSheet
import com.velocita.speedometer.ui.components.SpeedDisplay
import com.velocita.speedometer.ui.components.rememberOledPixelShiftModifier

/**
 * Root Jetpack Compose Screen connecting SpeedometerViewModel StateFlow with:
 * - Declarative Runtime Permission Handler (rememberLauncherForActivityResult)
 * - Non-intrusive Hardware GPS Disabled Banner (Settings.ACTION_LOCATION_SOURCE_SETTINGS)
 * - High-Contrast Tabular SpeedDisplay + HUD Mode + Settings BottomSheet
 */
@Composable
fun SpeedometerScreen(
    viewModel: SpeedometerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isSettingsOpen by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    HardwareWindowControllerEffect(
        keepScreenOn = uiState.keepScreenOn,
        screenBrightness = uiState.screenBrightness
    )

    val pixelShiftModifier = rememberOledPixelShiftModifier()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        when (uiState.screenStatus) {
            SpeedometerScreenStatus.PermissionDenied -> {
                PermissionDeniedContent(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                )
            }

            SpeedometerScreenStatus.Active -> {
                ActiveSpeedometerContent(
                    uiState = uiState,
                    pixelShiftModifier = pixelShiftModifier,
                    onOpenSettings = { isSettingsOpen = true },
                    onToggleHudQuick = { viewModel.toggleHudMode() },
                    onDoubleTapReset = { viewModel.resetSessionMetrics() },
                    onBrightnessDragDelta = { delta -> viewModel.adjustBrightnessByDelta(delta) }
                )
            }
        }

        if (isSettingsOpen) {
            SettingsBottomSheet(
                currentUnit = uiState.speedUnit,
                currentProfile = uiState.vehicleProfile,
                yellowThresholdKmh = uiState.thresholdYellowKmh,
                redThresholdKmh = uiState.thresholdRedKmh,
                smoothingAlpha = uiState.smoothingAlpha,
                keepScreenOn = uiState.keepScreenOn,
                isHudMirrored = uiState.isHudMirrored,
                onSelectUnit = viewModel::selectSpeedUnit,
                onSelectProfile = viewModel::selectVehicleProfile,
                onUpdateThresholds = viewModel::updateCustomThresholds,
                onUpdateSmoothing = viewModel::updateSmoothingFactor,
                onToggleKeepScreenOn = viewModel::setKeepScreenOn,
                onToggleHud = viewModel::toggleHudMode,
                onResetSession = viewModel::resetSessionMetrics,
                onDismiss = { isSettingsOpen = false }
            )
        }
    }
}

@Composable
private fun ActiveSpeedometerContent(
    uiState: SpeedometerUiState,
    pixelShiftModifier: Modifier,
    onOpenSettings: () -> Unit,
    onToggleHudQuick: () -> Unit,
    onDoubleTapReset: () -> Unit,
    onBrightnessDragDelta: (Float) -> Unit
) {
    val context = LocalContext.current
    val unit = uiState.speedUnit

    Box(modifier = Modifier.fillMaxSize()) {
        SpeedDisplay(
            speed = uiState.displayedSpeed,
            unitLabel = unit.displayLabel,
            accuracy = uiState.accuracyMeters,
            yellowThresholdDisplay = unit.convertFromKmh(uiState.thresholdYellowKmh),
            redThresholdDisplay = unit.convertFromKmh(uiState.thresholdRedKmh),
            isHudMode = uiState.isHudMirrored,
            isWeakSignal = uiState.isWeakSignal,
            isBatterySaverActive = uiState.isBatterySaverTriggered,
            maxSpeedDisplay = unit.convertFromKmh(uiState.sessionMetrics.maxSpeedKmh),
            avgSpeedDisplay = unit.convertFromKmh(uiState.sessionMetrics.movingAvgSpeedKmh),
            movingDurationSeconds = uiState.sessionMetrics.movingDurationSeconds,
            onDoubleTapReset = onDoubleTapReset,
            onBrightnessDragDelta = onBrightnessDragDelta,
            pixelShiftModifier = pixelShiftModifier
        )

        // FASE 1 (3.1): Non-intrusive Hardware GPS Disabled Banner
        if (uiState.isGpsDisabled) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .border(1.dp, Color(0xFFFF1744), RoundedCornerShape(8.dp))
                    .background(Color(0xFF140004), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.gps_disabled_banner),
                    color = Color(0xFFFFEA00),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = stringResource(id = R.string.gps_enable_action).uppercase(),
                    color = Color(0xFF00E676),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable {
                        try {
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        } catch (_: Exception) {
                        }
                    }
                )
            }
        }

        // Minimalist Bottom Control Pills (HUD Toggle, Vehicle Preset Indicator, & Settings Trigger)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (uiState.isHudMirrored) "[HUD: ON]" else "[HUD: OFF]",
                color = if (uiState.isHudMirrored) Color(0xFF00E676) else Color(0xFF555555),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .clickable { onToggleHudQuick() }
                    .padding(8.dp)
            )

            Text(
                text = uiState.vehicleProfile.id,
                color = Color(0xFF444444),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = "[CFG]",
                color = Color(0xFF666666),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .clickable { onOpenSettings() }
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun PermissionDeniedContent(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.permission_title),
            color = Color(0xFFFFEA00),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.permission_rationale),
            color = Color(0xFFAAAAAA),
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00E676),
                contentColor = Color(0xFF000000)
            )
        ) {
            Text(
                text = stringResource(id = R.string.permission_grant_button),
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }
    }
}
