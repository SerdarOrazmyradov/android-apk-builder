package com.example.sampleapp

import android.content.Context

object SmtpPreferences {
    private const val PREF_NAME = "smtp_settings"
    private const val KEY_SENDER_EMAIL = "sender_email"
    private const val KEY_APP_PASSWORD = "app_password"
    private const val KEY_RECEIVER_EMAIL = "receiver_email"

    fun saveSettings(context: Context, senderEmail: String, appPass: String, receiverEmail: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_SENDER_EMAIL, senderEmail.trim())
            putString(KEY_APP_PASSWORD, appPass.trim())
            putString(KEY_RECEIVER_EMAIL, receiverEmail.trim())
            apply()
        }
    }

    fun getSenderEmail(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_SENDER_EMAIL, "") ?: ""

    fun getAppPassword(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_APP_PASSWORD, "") ?: ""

    fun getReceiverEmail(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_RECEIVER_EMAIL, "") ?: ""
}