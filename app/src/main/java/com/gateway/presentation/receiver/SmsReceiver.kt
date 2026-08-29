package com.gateway.presentation.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import com.gateway.data.db.DatabaseHelper
import com.gateway.data.network.GeminiRepository

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync() // Acquire wake lock for async operations
        
        Thread {
            try {
                val bundle: Bundle = intent.extras ?: return@Thread
                val pdus = bundle.get("pdus") as? Array<*> ?: return@Thread

                for (pdu in pdus) {
                    val sms = SmsMessage.createFromPdu(pdu as ByteArray)
                    val sender = sms.originatingAddress ?: continue
                    val messageBody = sms.messageBody

                    Log.d(TAG, "SMS received from: $sender")

                    try {
                        val db = DatabaseHelper(context)
                        if (db.isUserAllowed(sender)) {
                            val apiKey = db.getApiKey()
                            if (apiKey.isNotEmpty()) {
                                // Use GeminiRepository with default model from models.json
                                GeminiRepository(context).askGemini(apiKey, messageBody, object : GeminiRepository.ApiCallback {
                                    override fun onSuccess(responseText: String) {
                                        Log.d(TAG, "AI Response: $responseText")
                                        sendSmsResponse(context, sender, responseText)
                                    }

                                    override fun onError(error: String) {
                                        Log.e(TAG, "AI Error: $error")
                                        sendSmsResponse(context, sender, "Error: $error")
                                    }
                                })
                            } else {
                                sendSmsResponse(context, sender, "API Key not configured")
                            }
                        } else {
                            Log.d(TAG, "Sender not in allowlist: $sender")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Database error: ${e.message}", e)
                        sendSmsResponse(context, sender, "System error: ${e.message}")
                    }
                }
            } finally {
                result.finish() // Release wake lock
            }
        }.start()
    }

    private fun sendSmsResponse(context: Context, phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            val sentIntents = ArrayList<android.app.PendingIntent>(parts.size)
            for (i in parts.indices) {
                val intent = Intent(SENT_SMS_ACTION)
                intent.putExtra("phoneNumber", phoneNumber)
                val sentIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    i,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                sentIntents.add(sentIntent)
            }
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, null)
            Log.d(TAG, "SMS sent to: $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "SmsReceiver"
        private const val SENT_SMS_ACTION = "com.gateway.SMS_SENT"
    }
}
