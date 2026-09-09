package io.turkmensms.aigateway.data

import java.util.Properties
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session
import javax.mail.Store
import javax.mail.UIDFolder
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class MailClient(private val config: BridgeConfig) {

    fun testSmtp(): Result<String> {
        return try {
            val session = smtpSession()
            val transport = session.getTransport("smtp")
            transport.connect(config.smtpHost, config.smtpPort, config.smtpUser, config.smtpPassword)
            transport.close()
            Result.success("SMTP connection OK")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun testImap(): Result<String> {
        var store: Store? = null
        return try {
            store = imapStore()
            val folder = store.getFolder(config.imapFolder.ifBlank { "INBOX" })
            folder.open(Folder.READ_ONLY)
            val count = folder.messageCount
            folder.close(false)
            Result.success("IMAP connection OK ($count messages)")
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                store?.close()
            } catch (_: Exception) {
            }
        }
    }

    fun sendMail(
        to: String,
        subject: String,
        body: String,
        replyTo: String? = null,
        inReplyTo: String? = null
    ) {
        val session = smtpSession()
        val message = MimeMessage(session)
        message.setFrom(InternetAddress(config.smtpUser))
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false))
        message.subject = subject
        message.setText(body, "UTF-8")
        if (!replyTo.isNullOrBlank()) {
            message.replyTo = arrayOf(InternetAddress(replyTo))
        }
        if (!inReplyTo.isNullOrBlank()) {
            message.setHeader("In-Reply-To", inReplyTo)
            message.setHeader("References", inReplyTo)
        }
        val transport = session.getTransport("smtp")
        try {
            transport.connect(config.smtpHost, config.smtpPort, config.smtpUser, config.smtpPassword)
            transport.sendMessage(message, message.allRecipients)
        } finally {
            try {
                transport.close()
            } catch (_: Exception) {
            }
        }
    }

    fun fetchUnseen(): List<InboundMail> {
        var store: Store? = null
        val result = ArrayList<InboundMail>()
        try {
            store = imapStore()
            val folder = store.getFolder(config.imapFolder.ifBlank { "INBOX" })
            folder.open(Folder.READ_WRITE)
            val uidFolder = folder as UIDFolder
            val messages = folder.search(javax.mail.search.FlagTerm(Flags(Flags.Flag.SEEN), false))
            for (message in messages) {
                val uid = uidFolder.getUID(message)
                if (uid <= config.lastImapUid) continue
                val from = extractFrom(message)
                result.add(
                    InboundMail(
                        uid = uid,
                        fromAddress = from,
                        subject = message.subject.orEmpty(),
                        body = extractText(message),
                        messageId = message.getHeader("Message-ID")?.firstOrNull().orEmpty()
                    )
                )
            }
            folder.close(false)
        } finally {
            try {
                store?.close()
            } catch (_: Exception) {
            }
        }
        return result.sortedBy { it.uid }
    }

    fun markRead(uid: Long) {
        var store: Store? = null
        try {
            store = imapStore()
            val folder = store.getFolder(config.imapFolder.ifBlank { "INBOX" })
            folder.open(Folder.READ_WRITE)
            val uidFolder = folder as UIDFolder
            val message = uidFolder.getMessageByUID(uid)
            if (message != null) {
                message.setFlag(Flags.Flag.SEEN, true)
            }
            folder.close(true)
        } finally {
            try {
                store?.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun smtpSession(): Session {
        val props = Properties()
        props["mail.smtp.host"] = config.smtpHost
        props["mail.smtp.port"] = config.smtpPort.toString()
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.connectiontimeout"] = "15000"
        props["mail.smtp.timeout"] = "20000"
        props["mail.smtp.ssl.protocols"] = "TLSv1.2"
        if (config.smtpStartTls) {
            props["mail.smtp.starttls.enable"] = "true"
            props["mail.smtp.starttls.required"] = "true"
            props["mail.smtp.ssl.trust"] = config.smtpHost
        }
        if (config.smtpPort == 465) {
            props["mail.smtp.ssl.enable"] = "true"
            props["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
            props["mail.smtp.ssl.trust"] = config.smtpHost
        }
        return Session.getInstance(props)
    }

    private fun imapStore(): Store {
        val props = Properties()
        props["mail.imap.host"] = config.imapHost
        props["mail.imap.port"] = config.imapPort.toString()
        props["mail.imap.connectiontimeout"] = "15000"
        props["mail.imap.timeout"] = "20000"
        val protocol = if (config.imapSsl) "imaps" else "imap"
        if (config.imapSsl) {
            props["mail.imaps.ssl.enable"] = "true"
            props["mail.imaps.ssl.trust"] = config.imapHost
            props["mail.imaps.ssl.checkserveridentity"] = "false"
            props["mail.imaps.ssl.protocols"] = "TLSv1.2"
        }
        val session = Session.getInstance(props)
        val store = session.getStore(protocol)
        store.connect(config.imapHost, config.imapPort, config.imapUser, config.imapPassword)
        return store
    }

    private fun extractFrom(message: Message): String {
        val addresses = message.from ?: return ""
        if (addresses.isEmpty()) return ""
        val addr = addresses[0]
        return if (addr is InternetAddress) {
            addr.address.orEmpty()
        } else {
            addr.toString()
        }
    }

    private fun extractText(message: Message): String {
        return try {
            extractPart(message.content)
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractPart(content: Any?): String {
        return when (content) {
            is String -> content
            is MimeMultipart -> {
                var text = ""
                var html = ""
                for (i in 0 until content.count) {
                    val bodyPart = content.getBodyPart(i)
                    val type = bodyPart.contentType.orEmpty().lowercase()
                    val inner = extractPart(bodyPart.content)
                    if (type.startsWith("text/plain") && text.isEmpty()) {
                        text = inner
                    } else if (type.startsWith("text/html") && html.isEmpty()) {
                        html = inner
                    } else if (text.isEmpty() && inner.isNotBlank()) {
                        text = inner
                    }
                }
                if (text.isNotBlank()) text else stripHtml(html)
            }
            else -> content?.toString().orEmpty()
        }
    }

    private fun stripHtml(html: String): String {
        return html.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
    }
}
