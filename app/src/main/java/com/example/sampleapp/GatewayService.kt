package com.example.sampleapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GatewayService : Service() {

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val senderPhone = intent?.getStringExtra("senderPhone")
        val messageBody = intent?.getStringExtra("messageBody")

        if (!senderPhone.isNullOrEmpty() && !messageBody.isNullOrEmpty()) {
            val user = AllowedUsers.getUser(senderPhone)
            if (user != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val aiResponse = GeminiApiClient.getAiResponse(
                        apiKey = user.apiKey,
                        model = user.model,
                        inputText = messageBody
                    )
                    sendSms(senderPhone, aiResponse)
                }
            }
        }

        return START_STICKY
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

    private fun startForegroundService() {
        val channelId = "ai_gateway_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "AI Gateway Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("AI SMS Gateway")
            .setContentText("7/24 arka fonda işläp dur...")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .build()

        startForeground(101, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}