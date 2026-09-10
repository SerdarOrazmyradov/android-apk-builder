package com.example.sampleapp

import java.util.Properties
import javax.mail.*
import javax.mail.search.FlagTerm

class EmailToSmsListener(
    private val email: String,
    private val appPassword: String,
    private val onNewReplyReceived: (phoneNumber: String, messageText: String) -> Unit
) {

    fun checkAndFetchReplies() {
        val props = Properties().apply {
            put("mail.store.protocol", "imaps")
            put("mail.imaps.host", "imap.gmail.com")
            put("mail.imaps.port", "993")
            put("mail.imaps.ssl.enable", "true")
        }

        try {
            val session = Session.getInstance(props, null)
            val store = session.getStore("imaps")
            store.connect("imap.gmail.com", email, appPassword)

            val inbox = store.getFolder("INBOX")
            inbox.open(Folder.READ_WRITE)

            // Diňe okalmandyk (UNSEEN) mailleri süzýäris
            val unreadMessages = inbox.search(FlagTerm(Flags(Flags.Flag.SEEN), false))

            for (message in unreadMessages) {
                val subject = message.subject ?: continue
                
                // Eger mail Sizden PC-de yza ugradylan reply bolsa
                if (subject.contains("SMS alert:") || subject.contains("Re:")) {
                    val phoneNumber = SmsParser.extractPhoneNumber(subject)
                    val bodyText = getMessageBody(message)

                    if (phoneNumber != null && bodyText.isNotBlank()) {
                        onNewReplyReceived(phoneNumber, bodyText)
                        
                        // Mail-i okaldy diýip bellemek (gaýtalanmazlygy üçin)
                        message.setFlag(Flags.Flag.SEEN, true)
                    }
                }
            }

            inbox.close(false)
            store.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getMessageBody(message: Message): String {
        if (message.isMimeType("text/plain")) {
            return message.content.toString()
        } else if (message.isMimeType("multipart/*")) {
            val multipart = message.content as Multipart
            for (i in 0 until multipart.count) {
                val bodyPart = multipart.getBodyPart(i)
                if (bodyPart.isMimeType("text/plain")) {
                    return bodyPart.content.toString()
                }
            }
        }
        return ""
    }
}