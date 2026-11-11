package com.velocita.speedometer.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.velocita.speedometer.domain.model.SessionMetrics
import com.velocita.speedometer.domain.model.VehicleProfile

/**
 * Section 4.2: Relational Local Database Schema (Zero-overhead SQLite implementation
 * matching the exact DDL for vehicle_profiles and transient_session_metrics without
 * annotation-processor bloat). Only invoked if optional local session persistence is enabled.
 */
class LocalSQLiteHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "velocita_local.db"
        private const val DATABASE_VERSION = 1

        const val DDL_VEHICLE_PROFILES = """
            CREATE TABLE IF NOT EXISTS vehicle_profiles (
                id TEXT PRIMARY KEY NOT NULL,
                profile_name TEXT NOT NULL,
                max_threshold REAL NOT NULL,
                yellow_threshold REAL NOT NULL,
                red_threshold REAL NOT NULL,
                filter_alpha REAL NOT NULL,
                is_default INTEGER NOT NULL DEFAULT 0
            );
        """

        const val DDL_TRANSIENT_SESSION_METRICS = """
            CREATE TABLE IF NOT EXISTS transient_session_metrics (
                session_id TEXT PRIMARY KEY NOT NULL,
                start_timestamp INTEGER NOT NULL,
                end_timestamp INTEGER,
                max_speed_kmh REAL NOT NULL,
                avg_speed_kmh REAL NOT NULL,
                total_distance_meters REAL NOT NULL,
                sample_count INTEGER NOT NULL,
                sync_status TEXT NOT NULL DEFAULT 'LOCAL_ONLY'
            );
        """

        const val DDL_INDEX_SESSION_START = """
            CREATE INDEX IF NOT EXISTS idx_session_start ON transient_session_metrics(start_timestamp);
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(DDL_VEHICLE_PROFILES.trimIndent())
        db.execSQL(DDL_TRANSIENT_SESSION_METRICS.trimIndent())
        db.execSQL(DDL_INDEX_SESSION_START.trimIndent())

        // Seed default presets (Bus Kota & Kereta Cepat)
        VehicleProfile.entries.forEach { profile ->
            val values = ContentValues().apply {
                put("id", profile.id)
                put("profile_name", profile.displayName)
                put("max_threshold", profile.maxScaleKmh)
                put("yellow_threshold", profile.defaultYellowThresholdKmh)
                put("red_threshold", profile.defaultRedThresholdKmh)
                put("filter_alpha", profile.defaultFilterAlpha)
                put("is_default", if (profile == VehicleProfile.TRAIN) 1 else 0)
            }
            db.insertWithOnConflict(
                "vehicle_profiles",
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS transient_session_metrics")
        db.execSQL("DROP TABLE IF EXISTS vehicle_profiles")
        onCreate(db)
    }

    fun saveSessionSummary(metrics: SessionMetrics, endTimestampMs: Long) {
        val values = ContentValues().apply {
            put("session_id", metrics.sessionId)
            put("start_timestamp", metrics.startTimestampEpochMs)
            put("end_timestamp", endTimestampMs)
            put("max_speed_kmh", metrics.maxSpeedKmh)
            put("avg_speed_kmh", metrics.movingAvgSpeedKmh)
            put("total_distance_meters", metrics.totalDistanceMeters)
            put("sample_count", metrics.sampleCount)
            put("sync_status", "LOCAL_ONLY")
        }
        writableDatabase.insertWithOnConflict(
            "transient_session_metrics",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }
}
