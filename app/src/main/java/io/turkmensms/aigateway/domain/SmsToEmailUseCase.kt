package io.turkmensms.aigateway.domain

import io.turkmensms.aigateway.data.BridgeConfig
import io.turkmensms.aigateway.data.BridgeLog
import io.turkmensms.aigateway.data.LogRepository
import io.turkmensms.aigateway.data.MailClient

class SmsToEmailUseCase(
    private val logs: LogRepository
) {
    fun execute(config: BridgeConfig, sender: String, body: String) {
        val phone = PhoneNormalizer.normalize(sender) ?: sender
        val subject = "SMS from $phone"
        val dest = config.effectiveForwardTo()
        logs.insert(BridgeLog.SMS_IN, phone, subject, body, BridgeLog.OK)
        if (dest.isBlank()) {
            logs.insert(BridgeLog.ERROR, phone, subject, body, BridgeLog.FAILED, "Forward-to address empty")
            return
        }
        try {
            MailClient(config).sendMail(
                to = dest,
                subject = subject,
                body = body,
                replyTo = config.smtpUser
            )
            logs.insert(BridgeLog.EMAIL_OUT, dest, subject, body, BridgeLog.OK)
        } catch (e: Exception) {
            logs.insert(
                BridgeLog.EMAIL_OUT,
                dest,
                subject,
                body,
                BridgeLog.FAILED,
                e.message ?: "SMTP send failed"
            )
            throw e
        }
    }
}
