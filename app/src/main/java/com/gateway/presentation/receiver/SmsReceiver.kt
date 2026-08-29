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
        val bundle: Bundle = intent.extras ?: return
        val pdus = bundle.get("pdus") as? Array<*> ?: return

        for (pdu in pdus) {
            val sms = SmsMessage.createFromPdu(pdu as ByteArray)
            val sender = sms.originatingAddress ?: continue
            val messageBody = sms.messageBody

            Log.d(TAG, "SMS received from: $sender")

            val db = DatabaseHelper(context)
            if (db.isUserAllowed(sender)) {
                val apiKey = db.getApiKey()
                if (apiKey.isNotEmpty()) {
                    GeminiRepository().askGemini(apiKey, messageBody, object : GeminiRepository.ApiCallback {
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
        }
    }

    private fun sendSmsResponse(context: Context, phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            val sentIntents = MutableList(parts.size) { null }
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, null)
            Log.d(TAG, "SMS sent to: $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
