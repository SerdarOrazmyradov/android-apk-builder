package com.example.sampleapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle = intent.extras ?: return
            val pdus = bundle.get("pdus") as Array<*>? ?: return

            for (pdu in pdus) {
                val sms = SmsMessage.createFromPdu(pdu as ByteArray)
                val senderPhone = sms.originatingAddress ?: continue
                val messageBody = sms.messageBody ?: continue

                // Diňe sanawda bar bolan telefon nomerleri barlaýarys
                val allowedUser = AllowedUsers.getUser(senderPhone)
                if (allowedUser != null) {
                    // Servise ugratmak
                    val serviceIntent = Intent(context, GatewayService::class.java).apply {
                        putExtra("senderPhone", senderPhone)
                        putExtra("messageBody", messageBody)
                    }
                    context.startService(serviceIntent)
                }
            }
        }
    }
}