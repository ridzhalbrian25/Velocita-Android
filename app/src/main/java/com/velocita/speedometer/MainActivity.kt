package com.velocita.speedometer

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.velocita.speedometer.di.ServiceLocator
import com.velocita.speedometer.presentation.SpeedometerViewModel
import com.velocita.speedometer.ui.SpeedometerScreen

/**
 * Section 2.1 & FASE 4 (3.12): Single Activity Architecture with Zero-Cold-Start Optimization
 * and immersive fullscreen display (supporting Quick Settings Tile launch over lock screen).
 */
class MainActivity : ComponentActivity() {

    private val viewModel: SpeedometerViewModel by viewModels {
        ServiceLocator.provideViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allow launching directly over Lock Screen from Quick Settings Tile (FASE 4 - 3.12)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        // Configure edge-to-edge immersive mode for maximum OLED canvas area
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            SpeedometerScreen(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissionAndStartTracking()
    }
}
