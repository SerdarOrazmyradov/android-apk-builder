package io.turkmensms.aigateway.data

import android.content.Context

class BridgeConfigStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val cipher = CredentialCipher(appContext)

    fun load(): BridgeConfig {
        val senders = prefs.getString(KEY_ALLOWED, "").orEmpty()
            .split('\n', ',', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return BridgeConfig(
            smtpHost = prefs.getString(KEY_SMTP_HOST, "smtp.gmail.com") ?: "smtp.gmail.com",
            smtpPort = prefs.getInt(KEY_SMTP_PORT, 587),
            smtpUser = prefs.getString(KEY_SMTP_USER, "") ?: "",
            smtpPassword = cipher.decrypt(prefs.getString(KEY_SMTP_PASS, "") ?: ""),
            smtpStartTls = prefs.getBoolean(KEY_SMTP_STARTTLS, true),
            imapHost = prefs.getString(KEY_IMAP_HOST, "imap.gmail.com") ?: "imap.gmail.com",
            imapPort = prefs.getInt(KEY_IMAP_PORT, 993),
            imapUser = prefs.getString(KEY_IMAP_USER, "") ?: "",
            imapPassword = cipher.decrypt(prefs.getString(KEY_IMAP_PASS, "") ?: ""),
            imapSsl = prefs.getBoolean(KEY_IMAP_SSL, true),
            imapFolder = prefs.getString(KEY_IMAP_FOLDER, "INBOX") ?: "INBOX",
            forwardTo = prefs.getString(KEY_FORWARD_TO, "") ?: "",
            allowedSenders = senders,
            pollIntervalSeconds = prefs.getInt(KEY_POLL, 30),
            bridgeEnabled = prefs.getBoolean(KEY_ENABLED, false),
            lastImapUid = prefs.getLong(KEY_LAST_UID, 0L)
        )
    }

    fun save(config: BridgeConfig) {
        prefs.edit()
            .putString(KEY_SMTP_HOST, config.smtpHost.trim())
            .putInt(KEY_SMTP_PORT, config.smtpPort)
            .putString(KEY_SMTP_USER, config.smtpUser.trim())
            .putString(KEY_SMTP_PASS, cipher.encrypt(config.smtpPassword))
            .putBoolean(KEY_SMTP_STARTTLS, config.smtpStartTls)
            .putString(KEY_IMAP_HOST, config.imapHost.trim())
            .putInt(KEY_IMAP_PORT, config.imapPort)
            .putString(KEY_IMAP_USER, config.imapUser.trim())
            .putString(KEY_IMAP_PASS, cipher.encrypt(config.imapPassword))
            .putBoolean(KEY_IMAP_SSL, config.imapSsl)
            .putString(KEY_IMAP_FOLDER, config.imapFolder.trim().ifEmpty { "INBOX" })
            .putString(KEY_FORWARD_TO, config.forwardTo.trim())
            .putString(KEY_ALLOWED, config.allowedSenders.joinToString("\n"))
            .putInt(KEY_POLL, config.clampedPollInterval())
            .putBoolean(KEY_ENABLED, config.bridgeEnabled)
            .apply()
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun setLastImapUid(uid: Long) {
        prefs.edit().putLong(KEY_LAST_UID, uid).apply()
    }

    companion object {
        private const val PREFS = "bridge_config"
        private const val KEY_SMTP_HOST = "smtp_host"
        private const val KEY_SMTP_PORT = "smtp_port"
        private const val KEY_SMTP_USER = "smtp_user"
        private const val KEY_SMTP_PASS = "smtp_pass"
        private const val KEY_SMTP_STARTTLS = "smtp_starttls"
        private const val KEY_IMAP_HOST = "imap_host"
        private const val KEY_IMAP_PORT = "imap_port"
        private const val KEY_IMAP_USER = "imap_user"
        private const val KEY_IMAP_PASS = "imap_pass"
        private const val KEY_IMAP_SSL = "imap_ssl"
        private const val KEY_IMAP_FOLDER = "imap_folder"
        private const val KEY_FORWARD_TO = "forward_to"
        private const val KEY_ALLOWED = "allowed_senders"
        private const val KEY_POLL = "poll_interval"
        private const val KEY_ENABLED = "bridge_enabled"
        private const val KEY_LAST_UID = "last_imap_uid"
    }
}
