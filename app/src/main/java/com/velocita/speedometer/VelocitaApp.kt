package com.velocita.speedometer

import android.app.Application
import com.velocita.speedometer.core.crash.OfflineCrashHandler

/**
 * Minimal Application entrypoint installing the zero-network OfflineCrashHandler (NFR 6.3).
 */
class VelocitaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        OfflineCrashHandler.install(this)
    }
}
