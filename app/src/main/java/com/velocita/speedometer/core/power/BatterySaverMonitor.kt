package com.velocita.speedometer.core.power

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * FASE 4 (3.11): Battery Saver Auto-Trigger & PowerManager Monitor.
 * Monitors Intent.ACTION_BATTERY_CHANGED and PowerManager.isPowerSaveMode.
 * When battery drops below 15% without active charging (or OS power save is active),
 * triggers battery saver mode: forces screen brightness to 20% and disables secondary accuracy computation.
 */
data class BatteryStatus(
    val batteryPct: Int = 100,
    val isCharging: Boolean = false,
    val isLowBatterySaverActive: Boolean = false
)

class BatterySaverMonitor(private val context: Context) {

    companion object {
        const val LOW_BATTERY_THRESHOLD_PCT = 15
        const val BATTERY_SAVER_BRIGHTNESS = 0.20f
    }

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun observeBatteryState(): Flow<BatteryStatus> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val batteryIntent = if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    intent
                } else {
                    context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                }

                val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 100

                val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

                val osPowerSave = try {
                    powerManager.isPowerSaveMode
                } catch (_: Exception) {
                    false
                }

                val lowBatteryAutoTrigger = (pct < LOW_BATTERY_THRESHOLD_PCT && !isCharging) || osPowerSave

                trySend(
                    BatteryStatus(
                        batteryPct = pct,
                        isCharging = isCharging,
                        isLowBatterySaverActive = lowBatteryAutoTrigger
                    )
                )
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        val initialSticky = context.registerReceiver(receiver, filter)
        if (initialSticky != null) {
            receiver.onReceive(context, initialSticky)
        }

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }
}
