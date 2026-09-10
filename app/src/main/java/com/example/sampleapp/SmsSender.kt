package com.example.sampleapp

import android.telephony.SmsManager

object SmsSender {
    fun sendSms(phoneNumber: String, message: String) {
        val smsManager = SmsManager.getDefault()
        
        // Eger sms uzyn bolsa bölüp ugradýar (Multipart)
        val parts = smsManager.divideMessage(message)
        if (parts.size > 1) {
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
        } else {
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        }
    }
}