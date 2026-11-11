package com.velocita.speedometer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocita.speedometer.core.crash.OfflineCrashHandler
import com.velocita.speedometer.domain.model.SpeedUnit
import com.velocita.speedometer.domain.model.VehicleProfile
import java.util.Locale

/**
 * FASE 2 (3.5), FASE 3 (3.8), & Section 6.3:
 * Modular Settings Bottom Sheet for:
 * - Multi-Unit Switcher (km/h, mph, knots, m/s)
 * - Vehicle Preset Modes (Bus Kota vs Kereta Cepat Whoosh vs Sepeda)
 * - Dual interactive Sliders for Yellow & Red threshold customization
 * - EMA Low-Pass Filter smoothing factor slider
 * - Keep Screen Awake & HUD Mirroring switches
 * - Offline Crash Log diagnostic viewer (NFR 6.3)
 */
@Composable
fun SettingsBottomSheet(
    currentUnit: SpeedUnit,
    currentProfile: VehicleProfile,
    yellowThresholdKmh: Float,
    redThresholdKmh: Float,
    smoothingAlpha: Float,
    keepScreenOn: Boolean,
    isHudMirrored: Boolean,
    onSelectUnit: (SpeedUnit) -> Unit,
    onSelectProfile: (VehicleProfile) -> Unit,
    onUpdateThresholds: (Float, Float) -> Unit,
    onUpdateSmoothing: (Float) -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    onToggleHud: () -> Unit,
    onResetSession: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var yellowSlider by remember(yellowThresholdKmh) { mutableFloatStateOf(yellowThresholdKmh) }
    var redSlider by remember(redThresholdKmh) { mutableFloatStateOf(redThresholdKmh) }
    var alphaSlider by remember(smoothingAlpha) { mutableFloatStateOf(smoothingAlpha) }
    var showCrashDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0A0A0A),
        contentColor = Color(0xFFEEEEEE)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "KONFIGURASI VELOCITA",
                color = Color(0xFF00E676),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            // 1. Speed Unit Switcher
            Text(
                text = "Satuan Kecepatan",
                color = Color(0xFFAAAAAA),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpeedUnit.entries.forEach { unit ->
                    val selected = unit == currentUnit
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = 1.dp,
                                color = if (selected) Color(0xFF00E676) else Color(0xFF333333),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .background(
                                color = if (selected) Color(0xFF003319) else Color(0xFF000000),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onSelectUnit(unit) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unit.displayLabel,
                            color = if (selected) Color(0xFF00E676) else Color(0xFF888888),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // 2. Vehicle Presets (FASE 3 - 3.8)
            Text(
                text = "Profil Moda Transportasi (Auto Threshold & Filter)",
                color = Color(0xFFAAAAAA),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                VehicleProfile.entries.forEach { profile ->
                    val selected = profile == currentProfile
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (selected) Color(0xFFFFEA00) else Color(0xFF222222),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .background(Color(0xFF050505), RoundedCornerShape(6.dp))
                            .clickable { onSelectProfile(profile) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = profile.displayName,
                                color = if (selected) Color(0xFFFFEA00) else Color(0xFFDDDDDD),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Kuning: ${profile.defaultYellowThresholdKmh.toInt()} | Merah: ${profile.defaultRedThresholdKmh.toInt()} km/h | α=${profile.defaultFilterAlpha}",
                                color = Color(0xFF777777),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (selected) {
                            Text(
                                text = "AKTIF",
                                color = Color(0xFFFFEA00),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // 3. Interactive Threshold Sliders (FASE 2 - 3.5)
            Column {
                Text(
                    text = "Ambang Peringatan Kuning: ${yellowSlider.toInt()} km/h",
                    color = Color(0xFFFFEA00),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
                Slider(
                    value = yellowSlider,
                    onValueChange = { newVal ->
                        yellowSlider = newVal
                        if (redSlider <= newVal + 5f) {
                            redSlider = (newVal + 10f).coerceAtMost(350f)
                        }
                    },
                    onValueChangeFinished = {
                        onUpdateThresholds(yellowSlider, redSlider)
                    },
                    valueRange = 10f..300f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFFEA00),
                        activeTrackColor = Color(0xFFFFEA00)
                    )
                )

                Text(
                    text = "Ambang Bahaya Merah: ${redSlider.toInt()} km/h",
                    color = Color(0xFFFF1744),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
                Slider(
                    value = redSlider,
                    onValueChange = { newVal ->
                        redSlider = newVal.coerceAtLeast(yellowSlider + 5f)
                    },
                    onValueChangeFinished = {
                        onUpdateThresholds(yellowSlider, redSlider)
                    },
                    valueRange = 20f..380f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF1744),
                        activeTrackColor = Color(0xFFFF1744)
                    )
                )
            }

            // 4. Low-Pass Filter Smoothing Alpha Slider (FASE 3 - 3.8)
            Column {
                Text(
                    text = String.format(Locale.US, "Low-Pass Filter Smoothing (α = %.2f)", alphaSlider),
                    color = Color(0xFFCCCCCC),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
                Slider(
                    value = alphaSlider,
                    onValueChange = { alphaSlider = it },
                    onValueChangeFinished = { onUpdateSmoothing(alphaSlider) },
                    valueRange = 0.05f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E676),
                        activeTrackColor = Color(0xFF00E676)
                    )
                )
            }

            // 5. Hardware & HUD Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mode Cermin HUD (Proyeksi Kaca)",
                    color = Color(0xFFDDDDDD),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
                Switch(
                    checked = isHudMirrored,
                    onCheckedChange = { onToggleHud() }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Layar Tetap Menyala (WakeLock)",
                    color = Color(0xFFDDDDDD),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
                Switch(
                    checked = keepScreenOn,
                    onCheckedChange = onToggleKeepScreenOn
                )
            }

            // 6. Action Buttons (Reset Session & Offline Crash Log Info)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        onResetSession()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Reset Sesi",
                        color = Color(0xFF00E676),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = { showCrashDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Info Log Offline",
                        color = Color(0xFFAAAAAA),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showCrashDialog) {
        val dumpText = remember { OfflineCrashHandler.readCrashDump(context) }
        AlertDialog(
            onDismissRequest = { showCrashDialog = false },
            containerColor = Color(0xFF111111),
            title = {
                Text(
                    text = "Status Diagnostik Offline",
                    color = Color(0xFF00E676),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp
                )
            },
            text = {
                Text(
                    text = dumpText,
                    color = Color(0xFFCCCCCC),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showCrashDialog = false }) {
                    Text("Tutup", color = Color(0xFF00E676))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        OfflineCrashHandler.clearCrashDump(context)
                        showCrashDialog = false
                    }
                ) {
                    Text("Bersihkan", color = Color(0xFF888888))
                }
            }
        )
    }
}
