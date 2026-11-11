package com.velocita.speedometer.domain.model

/**
 * FASE 3 (3.8) & Section 4.1/4.2: Vehicle Preset Profiles with Dynamic EMA Alpha & Thresholds.
 * - BUS (Mode Bus Kota / Mobil): Hijau (<50 km/h), Kuning (50-80 km/h), Merah (>80 km/h), alpha = 0.40
 * - TRAIN (Mode Kereta Antarkota / Kereta Cepat Whoosh): Hijau (<120 km/h), Kuning (120-250 km/h), Merah (>250 km/h), alpha = 0.15
 * - BIKE (Mode Sepeda / Commuter): Hijau (<20 km/h), Kuning (20-35 km/h), Merah (>35 km/h), alpha = 0.50
 * - CUSTOM (Mode Kustom Default MVP): Hijau (<60 km/h), Kuning (60-100 km/h), Merah (>100 km/h), alpha = 0.25
 */
enum class VehicleProfile(
    val id: String,
    val displayName: String,
    val defaultYellowThresholdKmh: Float,
    val defaultRedThresholdKmh: Float,
    val maxScaleKmh: Float,
    val defaultFilterAlpha: Float
) {
    BUS(
        id = "BUS",
        displayName = "Bus Kota / Mobil",
        defaultYellowThresholdKmh = 50.0f,
        defaultRedThresholdKmh = 80.0f,
        maxScaleKmh = 160.0f,
        defaultFilterAlpha = 0.40f
    ),
    TRAIN(
        id = "TRAIN",
        displayName = "Kereta Cepat / Antarkota",
        defaultYellowThresholdKmh = 120.0f,
        defaultRedThresholdKmh = 250.0f,
        maxScaleKmh = 400.0f,
        defaultFilterAlpha = 0.15f
    ),
    BIKE(
        id = "BIKE",
        displayName = "Sepeda / Mikro-Mobilitas",
        defaultYellowThresholdKmh = 20.0f,
        defaultRedThresholdKmh = 35.0f,
        maxScaleKmh = 60.0f,
        defaultFilterAlpha = 0.50f
    ),
    CUSTOM(
        id = "CUSTOM",
        displayName = "Standar / Kustom",
        defaultYellowThresholdKmh = 60.0f,
        defaultRedThresholdKmh = 100.0f,
        maxScaleKmh = 240.0f,
        defaultFilterAlpha = 0.25f
    );

    companion object {
        fun fromId(id: String?): VehicleProfile {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: TRAIN
        }
    }
}
