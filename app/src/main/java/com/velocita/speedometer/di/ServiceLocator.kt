package com.velocita.speedometer.di

import android.content.Context
import com.velocita.speedometer.core.location.AndroidLocationEngine
import com.velocita.speedometer.core.power.BatterySaverMonitor
import com.velocita.speedometer.data.local.LocalSQLiteHelper
import com.velocita.speedometer.data.preferences.UserPreferencesRepository
import com.velocita.speedometer.presentation.SpeedometerViewModel

/**
 * Section 2.1 & 2.3: Ultra-Lightweight Manual Dependency Injection / Service Locator.
 * Zero runtime reflection or Dagger/Hilt bytecode overhead for < 300ms cold-start SLA.
 */
object ServiceLocator {

    @Volatile
    private var locationEngine: AndroidLocationEngine? = null

    @Volatile
    private var preferencesRepository: UserPreferencesRepository? = null

    @Volatile
    private var batterySaverMonitor: BatterySaverMonitor? = null

    @Volatile
    private var localSQLiteHelper: LocalSQLiteHelper? = null

    fun provideLocationEngine(context: Context): AndroidLocationEngine {
        return locationEngine ?: synchronized(this) {
            locationEngine ?: AndroidLocationEngine(context.applicationContext).also {
                locationEngine = it
            }
        }
    }

    fun providePreferencesRepository(context: Context): UserPreferencesRepository {
        return preferencesRepository ?: synchronized(this) {
            preferencesRepository ?: UserPreferencesRepository(context.applicationContext).also {
                preferencesRepository = it
            }
        }
    }

    fun provideBatterySaverMonitor(context: Context): BatterySaverMonitor {
        return batterySaverMonitor ?: synchronized(this) {
            batterySaverMonitor ?: BatterySaverMonitor(context.applicationContext).also {
                batterySaverMonitor = it
            }
        }
    }

    fun provideLocalSQLiteHelper(context: Context): LocalSQLiteHelper {
        return localSQLiteHelper ?: synchronized(this) {
            localSQLiteHelper ?: LocalSQLiteHelper(context.applicationContext).also {
                localSQLiteHelper = it
            }
        }
    }

    fun provideViewModelFactory(context: Context): SpeedometerViewModel.Factory {
        val appContext = context.applicationContext
        return SpeedometerViewModel.Factory(
            locationEngine = provideLocationEngine(appContext),
            preferencesRepository = providePreferencesRepository(appContext),
            batterySaverMonitor = provideBatterySaverMonitor(appContext)
        )
    }
}
