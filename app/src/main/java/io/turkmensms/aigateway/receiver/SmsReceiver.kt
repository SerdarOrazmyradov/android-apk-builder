package io.turkmensms.aigateway.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import io.turkmensms.aigateway.service.BridgeForegroundService

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return
        val extras = intent.extras ?: return
        try {
            val pdus = extras.get("pdus") as? Array<*> ?: return
            val format = extras.getString("format")
            val body = StringBuilder()
            var sender = ""
            for (pdu in pdus) {
                if (pdu !is ByteArray) continue
                val sms = if (Build.VERSION.SDK_INT >= 23 && format != null) {
                    SmsMessage.createFromPdu(pdu, format)
                } else {
                    @Suppress("DEPRECATION")
                    SmsMessage.createFromPdu(pdu)
                }
                sender = sms.originatingAddress ?: sender
                body.append(sms.messageBody ?: "")
            }
            if (sender.isBlank() && body.isEmpty()) return
            val serviceIntent = Intent(context, BridgeForegroundService::class.java).apply {
                action = BridgeForegroundService.ACTION_FORWARD_SMS
                putExtra(BridgeForegroundService.EXTRA_SENDER, sender)
                putExtra(BridgeForegroundService.EXTRA_BODY, body.toString())
            }
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (_: Exception) {
        }
    }
}
