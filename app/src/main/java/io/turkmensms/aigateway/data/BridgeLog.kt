package io.turkmensms.aigateway.data

data class BridgeLog(
    val id: Long = 0,
    val timestamp: Long,
    val direction: String,
    val peer: String,
    val subject: String,
    val snippet: String,
    val status: String,
    val detail: String
) {
    companion object {
        const val SMS_IN = "SMS_IN"
        const val EMAIL_IN = "EMAIL_IN"
        const val SMS_OUT = "SMS_OUT"
        const val EMAIL_OUT = "EMAIL_OUT"
        const val SKIP = "SKIP"
        const val ERROR = "ERROR"
        const val OK = "OK"
        const val FAILED = "FAILED"
        const val SKIPPED = "SKIPPED"
    }
}
