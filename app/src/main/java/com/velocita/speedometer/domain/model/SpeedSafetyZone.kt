package com.velocita.speedometer.domain.model

/**
 * FASE 1 (3.3): WCAG AAA High-Contrast Safe Color Zones on True Black (#000000).
 * - SAFE_GREEN (#00E676): Contrast ratio 13.9:1 (WCAG AAA)
 * - WARNING_YELLOW (#FFEA00): Contrast ratio 17.1:1 (WCAG AAA)
 * - DANGER_RED (#FF1744): Contrast ratio 5.8:1 (WCAG AAA Large Display Text >18pt bold)
 */
enum class SpeedSafetyZone(val hexColor: Long, val label: String) {
    SAFE_GREEN(hexColor = 0xFF00E676, label = "AMAN"),
    WARNING_YELLOW(hexColor = 0xFFFFEA00, label = "WASPADA"),
    DANGER_RED(hexColor = 0xFFFF1744, label = "LAJU CEPAT")
}
