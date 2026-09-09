package io.turkmensms.aigateway.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import io.turkmensms.aigateway.R
import io.turkmensms.aigateway.data.BridgeConfigStore
import io.turkmensms.aigateway.data.BridgeLog
import io.turkmensms.aigateway.data.LogRepository
import io.turkmensms.aigateway.data.MailClient
import io.turkmensms.aigateway.domain.EmailToSmsUseCase
import io.turkmensms.aigateway.domain.SmsToEmailUseCase
import io.turkmensms.aigateway.ui.HomeActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class BridgeForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var pollJob: Job? = null
    private lateinit var store: BridgeConfigStore
    private lateinit var logs: LogRepository
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        store = BridgeConfigStore(this)
        logs = LogRepository(this)
        running.set(true)
        lastError.set("")
        ensureChannel(this)
        startForeground(NOTIFICATION_ID, buildNotification("SMS-Gmail bridge running"))
        startPollLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_FORWARD_SMS) {
            val sender = intent.getStringExtra(EXTRA_SENDER).orEmpty()
            val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
            scope.launch {
                handleIncomingSms(sender, body)
            }
        }
        if (intent?.action == ACTION_STOP) {
            store.setEnabled(false)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running.set(false)
        pollJob?.cancel()
        scope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startPollLoop() {
        pollJob = scope.launch {
            var backoffMs = 0L
            while (isActive) {
                val config = store.load()
                if (!config.bridgeEnabled || !config.isReady()) {
                    delay(config.clampedPollInterval() * 1000L)
                    continue
                }
                if (backoffMs > 0) {
                    delay(backoffMs)
                }
                val ok = mutex.withLock {
                    withWakeLock {
                        try {
                            EmailToSmsUseCase(store, logs).poll(config)
                            lastPollAt.set(System.currentTimeMillis())
                            lastError.set("")
                            true
                        } catch (e: Exception) {
                            val msg = e.message ?: "IMAP poll failed"
                            lastError.set(msg)
                            logs.insert(BridgeLog.ERROR, "", "", "", BridgeLog.FAILED, msg)
                            false
                        }
                    }
                }
                backoffMs = if (ok) {
                    0L
                } else {
                    when (backoffMs) {
                        0L -> 5_000L
                        5_000L -> 15_000L
                        else -> 45_000L
                    }
                }
                delay(store.load().clampedPollInterval() * 1000L)
            }
        }
    }

    private suspend fun handleIncomingSms(sender: String, body: String) {
        mutex.withLock {
            withWakeLock {
                val config = store.load()
                if (!config.isReady()) {
                    logs.insert(
                        BridgeLog.ERROR,
                        sender,
                        "SMS from $sender",
                        body,
                        BridgeLog.FAILED,
                        "SMTP not configured"
                    )
                    return@withWakeLock
                }
                try {
                    SmsToEmailUseCase(logs).execute(config, sender, body)
                    lastError.set("")
                } catch (e: Exception) {
                    lastError.set(e.message ?: "SMS forward failed")
                }
            }
        }
    }

    private fun <T> withWakeLock(block: () -> T): T {
        acquireWakeLock()
        try {
            return block()
        } finally {
            releaseWakeLock()
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "smsbridge:poll").apply {
                setReferenceCounted(false)
            }
        }
        wakeLock?.acquire(60_000L)
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
    }

    private fun buildNotification(text: String): Notification {
        val launch = Intent(this, HomeActivity::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (android.os.Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        val pending = PendingIntent.getActivity(this, 0, launch, flags)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentIntent(pending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val ACTION_FORWARD_SMS = "io.turkmensms.aigateway.FORWARD_SMS"
        const val ACTION_STOP = "io.turkmensms.aigateway.STOP"
        const val EXTRA_SENDER = "sender"
        const val EXTRA_BODY = "body"
        const val CHANNEL_ID = "bridge_channel"
        const val NOTIFICATION_ID = 42

        private val running = AtomicBoolean(false)
        private val lastPollAt = AtomicLong(0L)
        private val lastError = AtomicReference("")

        fun isRunning(): Boolean = running.get()
        fun lastPollMillis(): Long = lastPollAt.get()
        fun lastErrorMessage(): String = lastError.get().orEmpty()

        fun start(context: Context) {
            val intent = Intent(context, BridgeForegroundService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BridgeForegroundService::class.java)
            intent.action = ACTION_STOP
            context.startService(intent)
        }

        fun ensureChannel(context: Context) {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                val manager = context.getSystemService(android.app.NotificationManager::class.java)
                val channel = android.app.NotificationChannel(
                    CHANNEL_ID,
                    "Bridge",
                    android.app.NotificationManager.IMPORTANCE_LOW
                )
                manager.createNotificationChannel(channel)
            }
        }
    }
}
