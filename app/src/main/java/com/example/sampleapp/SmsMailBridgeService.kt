package com.example.sampleapp

import kotlinx.coroutines.*

class SmsMailBridgeService {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startListening() {
        scope.launch {
            val listener = EmailToSmsListener(
                email = SmtpPreferences.getSenderEmail(context),
                appPassword = SmtpPreferences.getAppPassword(context)
            ) { phoneNumber, replyText ->
                
                // Täze reply gelende SMS ugradýarys
                SmsSender.sendSms(phoneNumber, replyText)
            }

            // Her 30 sekuntdan mailleri barlap durýar
            while (isActive) {
                listener.checkAndFetchReplies()
                delay(30_000) 
            }
        }
    }

    fun stop() {
        scope.cancel()
    }
}