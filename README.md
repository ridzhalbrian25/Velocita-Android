# Velocita: Ultra-Lightweight Offline GPS Speedometer & HUD

Implementasi lengkap proyek Android **Velocita** berdasarkan spesifikasi `PRD-UltraLite_Speedometer_Android.md`, `cursorrules.txt`, dan `Tasks-UltraLite_Speedometer_Android.csv` (Fase 1 hingga Fase 4).

## Ringkasan Arsitektur & Fitur

- **Fase 1 (Core Engine & Essential MVP):**
  - `AndroidLocationEngine.kt` & `HaversineCalculator.kt`: Pembacaan kecepatan real-time murni via `android.location.LocationManager` (`ACCESS_FINE_LOCATION`) tanpa dependensi Google Play Services, dilengkapi fallback rumus Haversine dan *Sensory Sandboxing* (koordinat tidak pernah disimpan ke disk).
  - `SpeedDisplay.kt` & `SpeedometerScreen.kt`: Antarmuka Jetpack Compose dengan latar belakang **OLED True Black (`#000000`)**, angka raksasa tabular (`fontFeatureSettings = "tnum"`), dan penanganan izin runtime deklaratif serta banner status GPS non-intrusif.
  - `EvaluateThresholdUseCase.kt`: Transisi warna dinamis berstandar aksesibilitas **WCAG AAA** (`#00E676` Hijau Aman, `#FFEA00` Kuning Neon, `#FF1744` Merah Waspada) dengan animasi `FastOutSlowInEasing` 250ms.

- **Fase 2 (Hardware Control, Precision & Local Settings):**
  - `WakeLockAndPixelShifter.kt`: Kontrol `FLAG_KEEP_SCREEN_ON`, rotasi responsif otomatis (`Row` pada lanskap, `Column` pada potret) tanpa re-kreasi Activity, dan **OLED Pixel Shifter Engine** (translasi 1–2 dp setiap 120 detik).
  - `UserPreferencesRepository.kt` & `SettingsBottomSheet.kt`: Konversi multi-satuan (`km/h`, `mph`, `knots`, `m/s`), kustomisasi Slider ambang batas Kuning/Merah, serta persistensi Jetpack DataStore Preferences (`user_preferences`). Dilengkapi juga dengan `LocalSQLiteHelper.kt` sesuai skema DDL Bagian 4.2.
  - Indikator Akurasi Satelit (`ACC: ±m`) dan deteksi sinyal lemah/terowongan (>25m atau >4 detik tanpa sinyal memudarkan digit ke `alpha = 0.35f` dan menampilkan `[SAT LOST]`).

- **Fase 3 (Environmental Adaptation & Session Telemetry):**
  - Mode **Heads-Up Display (HUD)** dengan pembalikan horizontal (`graphicsLayer { scaleX = -1f }`) dan gestur usap vertikal di sisi kanan layar untuk mengatur kecerahan (`0.01f` – `1.0f`).
  - `VehicleProfile.kt` & `ApplyLowPassFilterUseCase.kt`: Preset instan **Bus Kota / Mobil** ($lpha = 0.40$) dan **Kereta Cepat / Antarkota** ($lpha = 0.15$) dengan algoritma Exponential Moving Average (EMA).
  - `SessionMetricsTracker.kt`: Kalkulasi transien in-memory (`MAX`, `AVG` saat $v > 2.0	ext{ km/h}$, dan durasi bergerak) dengan fitur *Double-Tap Reset* + umpan balik getar haptic (`HapticFeedbackType.LongPress`).

- **Fase 4 (Extreme Optimization & OS Deep Integration):**
  - `app/build.gradle.kts`, `app/proguard-rules.pro`, & `scripts/subset_font.py`: Optimasi ukuran binary `< 3MB` via R8 full mode, penghapusan `Log` & `Intrinsics`, ABI split (`arm64-v8a`, `armeabi-v7a`), dan pemangkasan font.
  - Adaptive Polling Frequency (`1000ms` $ightarrow$ `4000ms` saat stasioner $>30$ detik) dan `BatterySaverMonitor.kt` (auto-trigger kecerahan 20% saat baterai $<15\%$).
  - `SpeedometerTileService.kt` (Quick Settings Tile) dan `styles.xml` Zero-Cold-Start (`windowBackground = @android:color/black`).
