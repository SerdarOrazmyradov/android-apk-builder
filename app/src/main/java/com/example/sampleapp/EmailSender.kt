package com.example.sampleapp

import android.content.Context
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailSender {

    fun sendEmail(context: Context, subject: String, body: String) {
        val senderEmail = SmtpPreferences.getSenderEmail(context)
        val appPassword = SmtpPreferences.getAppPassword(context)
        val receiverEmail = SmtpPreferences.getReceiverEmail(context)

        if (senderEmail.isEmpty() || appPassword.isEmpty() || receiverEmail.isEmpty()) {
            LogManager.log(context, "EmailSender", "SMTP sazlamalary doldurylmady!")
            return
        }

        val props = Properties().apply {
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.socketFactory.port", "465")
            put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
            put("mail.smtp.auth", "true")
            put("mail.smtp.port", "465")
            // Android 5.1 TLS1.2 goldawy üçin:
            put("mail.smtp.ssl.protocols", "TLSv1.2")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(senderEmail, appPassword)
            }
        })

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(senderEmail))
            addRecipient(Message.RecipientType.TO, InternetAddress(receiverEmail))
            this.subject = subject
            setText(body)
        }

        Transport.send(message)
        LogManager.log(context, "EmailSender", "Email üstünlikli ugradyldy: $receiverEmail")
    }
}