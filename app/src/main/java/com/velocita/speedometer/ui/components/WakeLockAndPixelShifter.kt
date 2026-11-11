package com.velocita.speedometer.ui.components

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * FASE 2 (3.4) & FASE 3 (3.7):
 * - Keep Screen Awake (FLAG_KEEP_SCREEN_ON) via DisposableEffect
 * - Dynamic Window Brightness modulation via vertical gesture or Battery Saver
 * - OLED Burn-In Prevention (Pixel Shifter Engine) shifting 1-2dp every 120 seconds
 */
@Composable
fun HardwareWindowControllerEffect(
    keepScreenOn: Boolean,
    screenBrightness: Float = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
) {
    val context = LocalContext.current

    DisposableEffect(keepScreenOn) {
        val window = (context as? Activity)?.window
        if (keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Memastikan aplikasi tidak mengunci kecerahan layar (mengikuti preferensi/auto-brightness sistem)
    LaunchedEffect(Unit) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val params = window.attributes
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = params
    }
}

/**
 * FASE 2 (3.4): OLED Burn-In Prevention Modifier.
 * Every 120 seconds, shifts the entire UI container by -2..2 dp on X and Y axes
 * to rotate active sub-pixel load on AMOLED panels.
 */
@Composable
fun rememberOledPixelShiftModifier(intervalMs: Long = 120_000L): Modifier {
    var offsetXDp by remember { mutableIntStateOf(0) }
    var offsetYDp by remember { mutableIntStateOf(0) }

    LaunchedEffect(intervalMs) {
        val offsets = intArrayOf(-2, -1, 1, 2)
        while (true) {
            delay(intervalMs)
            offsetXDp = offsets[Random.nextInt(offsets.size)]
            offsetYDp = offsets[Random.nextInt(offsets.size)]
        }
    }

    return Modifier.offset(x = offsetXDp.dp, y = offsetYDp.dp)
}
