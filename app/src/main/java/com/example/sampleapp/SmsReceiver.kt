package com.example.sampleapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    private val TAG = "SmsReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            var smsSender = ""
            var smsBody = ""

            // Android wersiýasyna görä SMS-i okaýarys
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                for (smsMessage in messages) {
                    smsSender = smsMessage.displayOriginatingAddress ?: ""
                    smsBody += smsMessage.messageBody ?: ""
                }
            } else {
                val smsBundle = intent.extras
                if (smsBundle != null) {
                    val pdus = smsBundle.get("pdus") as? Array<*>
                    if (pdus == null) return
                    
                    val messages = arrayOfNulls<SmsMessage>(pdus.size)
                    for (i in messages.indices) {
                        messages[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray)
                        smsBody += messages[i]?.messageBody ?: ""
                    }
                    smsSender = messages[0]?.originatingAddress ?: ""
                }
            }

            LogManager.log(context, TAG, "Täze SMS geldi -> Tel: $smsSender | Tekst: $smsBody")

            val senderPhone = smsSender
            val messageBody = smsBody

            val user = AllowedUsers.getUser(senderPhone)
            if (SmtpPreferences.getSenderEmail(context).isNotEmpty() && SmtpPreferences.getAppPassword(context).isNotEmpty() && SmtpPreferences.getReceiverEmail(context).isNotEmpty()) {
                if (user != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val subject = "SMS alert: ${user.name} ($senderPhone)"
                            val body = "Iberiji: $senderPhone\nSms Tekst:\n$messageBody"
                            // EmailSender çagyrýarys
                            EmailSender.sendEmail(context, subject, body)
                        }
                        catch (e: Exception) {
                            LogManager.log(context, TAG, "Email iberilende säwlik: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                } else {
                    LogManager.log(context, TAG, "Belgi rugsat berlen däl: $senderPhone")
                }
            } else {
                LogManager.log(context, TAG, "Email sazlamalary ýatda saklanmady")
            }
            
        }
    }

    private fun sendSms(phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}