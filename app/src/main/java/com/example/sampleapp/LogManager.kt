package com.example.sampleapp

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogManager {
    private const val LOG_FILE_NAME = "app_logs.txt"

    fun log(context: Context, tag: String, message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val logLine = "[$timestamp] [$tag]: $message\n"

        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            file.appendText(logLine)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun readLogs(context: Context): String {
        return try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists() && file.readText().isNotEmpty()) file.readText() else "Hiç hili log ýok."
        } catch (e: Exception) {
            "Loglary okap bolmady: ${e.message}"
        }
    }

    fun clearLogs(context: Context) {
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) file.writeText("")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}