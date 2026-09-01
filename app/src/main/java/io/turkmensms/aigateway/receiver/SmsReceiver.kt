package io.turkmensms.aigateway.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import io.turkmensms.aigateway.data.network.GeminiApiClient
import io.turkmensms.aigateway.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle = intent.extras
            if (bundle != null) {
                try {
                    val pdus = bundle.get("pdus") as Array<*>?
                    if (pdus != null) {
                        val userRepository = UserRepository(context)
                        val fullMessage = StringBuilder()
                        var senderNumber = ""

                        // Extract SMS message and sender
                        for (pdu in pdus) {
                            val sms = SmsMessage.createFromPdu(pdu as ByteArray)
                            senderNumber = sms.originatingAddress ?: ""
                            fullMessage.append(sms.messageBody)
                        }

                        Log.d(TAG, "Incoming SMS from: $senderNumber")

                        // Check if sender is in allowed list
                        if (userRepository.isUserAllowed(senderNumber)) {
                            Log.d(TAG, "Sender $senderNumber is allowed. Processing message.")
                            processMessage(context, senderNumber, fullMessage.toString())
                        } else {
                            Log.d(TAG, "Sender $senderNumber is NOT allowed. Ignoring.")
                            // Optionally send rejection SMS
                            sendSms(
                                context,
                                senderNumber,
                                "Access denied. You are not in the allowed users list."
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing SMS: ${e.localizedMessage}", e)
                }
            }
        }
    }

    private fun processMessage(context: Context, phoneNumber: String, message: String) {
        val userRepository = UserRepository(context)
        val apiKey = userRepository.getApiKeyForUser(phoneNumber)

        if (apiKey.isNullOrBlank()) {
            Log.e(TAG, "No API key found for user $phoneNumber")
            sendSms(context, phoneNumber, "Error: No API key configured.")
            return
        }

        // Process in coroutine to avoid blocking
        GlobalScope.launch(Dispatchers.Default) {
            try {
                val geminiClient = GeminiApiClient(apiKey)
                val result = geminiClient.sendMessage(message)

                result.onSuccess { response ->
                    Log.d(TAG, "Gemini response: $response")
                    sendSms(context, phoneNumber, response)
                }.onFailure { error ->
                    Log.e(TAG, "Gemini API error: ${error.localizedMessage}")
                    sendSms(context, phoneNumber, "Error: ${error.localizedMessage}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in message processing: ${e.localizedMessage}", e)
                sendSms(context, phoneNumber, "Error processing your message.")
            }
        }
    }

    private fun sendSms(context: Context, phoneNumber: String, message: String) {
        return try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            }
            Log.d(TAG, "SMS sent to $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS: ${e.localizedMessage}", e)
        }
    }
}
