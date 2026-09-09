package io.turkmensms.aigateway.data

import android.telephony.SmsManager

class SmsSender {
    fun send(phone: String, text: String): Result<Unit> {
        return try {
            val smsManager = SmsManager.getDefault()
            val body = text.ifEmpty { " " }
            val parts = smsManager.divideMessage(body)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(phone, null, body, null, null)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
