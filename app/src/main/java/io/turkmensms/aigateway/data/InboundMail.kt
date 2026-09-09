package io.turkmensms.aigateway.data

data class InboundMail(
    val uid: Long,
    val fromAddress: String,
    val subject: String,
    val body: String,
    val messageId: String
)
