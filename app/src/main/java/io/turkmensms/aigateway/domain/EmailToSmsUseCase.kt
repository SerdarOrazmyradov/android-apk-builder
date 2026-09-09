package io.turkmensms.aigateway.domain

import io.turkmensms.aigateway.data.BridgeConfig
import io.turkmensms.aigateway.data.BridgeConfigStore
import io.turkmensms.aigateway.data.BridgeLog
import io.turkmensms.aigateway.data.InboundMail
import io.turkmensms.aigateway.data.LogRepository
import io.turkmensms.aigateway.data.MailClient
import io.turkmensms.aigateway.data.SmsSender

class EmailToSmsUseCase(
    private val store: BridgeConfigStore,
    private val logs: LogRepository,
    private val smsSender: SmsSender = SmsSender()
) {
    fun poll(config: BridgeConfig) {
        val client = MailClient(config)
        val mails = client.fetchUnseen()
        var maxUid = config.lastImapUid
        for (mail in mails) {
            processOne(config, client, mail)
            if (mail.uid > maxUid) maxUid = mail.uid
        }
        if (maxUid > config.lastImapUid) {
            store.setLastImapUid(maxUid)
        }
    }

    private fun processOne(config: BridgeConfig, client: MailClient, mail: InboundMail) {
        val allowed = config.allowedSenderSet()
        val from = mail.fromAddress.trim().lowercase()
        if (allowed.isEmpty() || from !in allowed) {
            safeMarkRead(client, mail.uid)
            logs.insert(
                BridgeLog.SKIP,
                mail.fromAddress,
                mail.subject,
                mail.body,
                BridgeLog.SKIPPED,
                "Sender not allowed"
            )
            return
        }

        logs.insert(BridgeLog.EMAIL_IN, mail.fromAddress, mail.subject, mail.body, BridgeLog.OK)
        when (val intent = MessageParser.parse(mail.subject, mail.body)) {
            is InboundIntent.Question -> {
                val smsResult = smsSender.send(intent.phone, intent.body)
                val ok = smsResult.isSuccess
                logs.insert(
                    BridgeLog.SMS_OUT,
                    intent.phone,
                    mail.subject,
                    intent.body,
                    if (ok) BridgeLog.OK else BridgeLog.FAILED,
                    smsResult.exceptionOrNull()?.message.orEmpty()
                )
                val answerBody = if (ok) {
                    "SENT ${intent.phone}"
                } else {
                    "FAILED ${intent.phone}: ${smsResult.exceptionOrNull()?.message ?: "SMS send failed"}"
                }
                try {
                    client.sendMail(
                        to = mail.fromAddress,
                        subject = "ANSWER",
                        body = answerBody,
                        replyTo = config.smtpUser,
                        inReplyTo = mail.messageId.ifBlank { null }
                    )
                    logs.insert(BridgeLog.EMAIL_OUT, mail.fromAddress, "ANSWER", answerBody, BridgeLog.OK)
                } catch (e: Exception) {
                    logs.insert(
                        BridgeLog.EMAIL_OUT,
                        mail.fromAddress,
                        "ANSWER",
                        answerBody,
                        BridgeLog.FAILED,
                        e.message ?: "ANSWER send failed"
                    )
                }
                safeMarkRead(client, mail.uid)
            }
            is InboundIntent.SmsReply -> {
                val smsResult = smsSender.send(intent.phone, intent.body)
                logs.insert(
                    BridgeLog.SMS_OUT,
                    intent.phone,
                    mail.subject,
                    intent.body,
                    if (smsResult.isSuccess) BridgeLog.OK else BridgeLog.FAILED,
                    smsResult.exceptionOrNull()?.message.orEmpty()
                )
                safeMarkRead(client, mail.uid)
            }
            InboundIntent.Skip -> {
                safeMarkRead(client, mail.uid)
                logs.insert(
                    BridgeLog.SKIP,
                    mail.fromAddress,
                    mail.subject,
                    mail.body,
                    BridgeLog.SKIPPED,
                    "Subject not recognized"
                )
            }
        }
    }

    private fun safeMarkRead(client: MailClient, uid: Long) {
        try {
            client.markRead(uid)
        } catch (e: Exception) {
            logs.insert(BridgeLog.ERROR, "", "", "", BridgeLog.FAILED, e.message ?: "markRead failed")
        }
    }
}
