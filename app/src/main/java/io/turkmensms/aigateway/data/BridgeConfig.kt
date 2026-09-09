package io.turkmensms.aigateway.data

data class BridgeConfig(
    val smtpHost: String = "smtp.gmail.com",
    val smtpPort: Int = 587,
    val smtpUser: String = "",
    val smtpPassword: String = "",
    val smtpStartTls: Boolean = true,
    val imapHost: String = "imap.gmail.com",
    val imapPort: Int = 993,
    val imapUser: String = "",
    val imapPassword: String = "",
    val imapSsl: Boolean = true,
    val imapFolder: String = "INBOX",
    val forwardTo: String = "",
    val allowedSenders: List<String> = emptyList(),
    val pollIntervalSeconds: Int = 30,
    val bridgeEnabled: Boolean = false,
    val lastImapUid: Long = 0L
) {
    fun effectiveForwardTo(): String {
        val dest = forwardTo.trim()
        return dest.ifEmpty { smtpUser.trim() }
    }

    fun clampedPollInterval(): Int = pollIntervalSeconds.coerceIn(10, 300)

    fun allowedSenderSet(): Set<String> =
        allowedSenders.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

    fun isReady(): Boolean {
        return smtpHost.isNotBlank() &&
            smtpUser.isNotBlank() &&
            smtpPassword.isNotBlank() &&
            imapHost.isNotBlank() &&
            imapUser.isNotBlank() &&
            imapPassword.isNotBlank()
    }
}
