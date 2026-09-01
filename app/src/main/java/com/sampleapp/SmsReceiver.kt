package com.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
import com.sampleapp.service.SmsService

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle = intent.extras
            if (bundle != null) {
                try {
                    val pdus = bundle.get("pdus") as Array<*>?
                    if (pdus != null) {
                        val fullMessage = StringBuilder()
                        var senderNumber = ""

                        for (pdu in pdus) {
                            val sms = SmsMessage.createFromPdu(pdu as ByteArray)
                            senderNumber = sms.originatingAddress ?: ""
                            fullMessage.append(sms.messageBody)
                        }

                        Log.d("SmsReceiver", "Gelen SMS: $senderNumber -> $fullMessage")

                        // Servis arkaly Backend-e ugratmak
                        val smsService = SmsService(context)
                        smsService.forwardSmsToBackend(senderNumber, fullMessage.toString())
                    }
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "SMS okamakda säwlik: ${e.localizedMessage}")
                }
            }
        }
    }
}