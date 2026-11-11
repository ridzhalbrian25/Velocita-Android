package com.velocita.speedometer.core.crash

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Section 6.3: Offline Crash Handling Architecture.
 * Implements Thread.setDefaultUncaughtExceptionHandler to write fatal anomalies to
 * private local file (`crash_dump.txt`) readable transparently by the user in Settings.
 */
class OfflineCrashHandler(private val appContext: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    companion object {
        const val CRASH_FILE_NAME = "crash_dump.txt"

        fun install(context: Context) {
            val appCtx = context.applicationContext
            Thread.setDefaultUncaughtExceptionHandler(OfflineCrashHandler(appCtx))
        }

        fun readCrashDump(context: Context): String {
            return try {
                val file = File(context.filesDir, CRASH_FILE_NAME)
                if (file.exists()) file.readText() else "Tidak ada riwayat crash tercatat (0 anomali)."
            } catch (_: Exception) {
                "Gagal membaca berkas log lokal."
            }
        }

        fun clearCrashDump(context: Context) {
            try {
                val file = File(context.filesDir, CRASH_FILE_NAME)
                if (file.exists()) file.delete()
            } catch (_: Exception) {
            }
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            pw.flush()

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val report = buildString {
                appendLine("=== VELOCITA OFFLINE CRASH DUMP ===")
                appendLine("Timestamp: $timestamp")
                appendLine("Thread: ${thread.name}")
                appendLine("Message: ${throwable.localizedMessage}")
                appendLine("Stacktrace:")
                appendLine(sw.toString().take(4000))
            }
            File(appContext.filesDir, CRASH_FILE_NAME).writeText(report)
        } catch (_: Exception) {
            // Must never throw inside UncaughtExceptionHandler
        } finally {
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
