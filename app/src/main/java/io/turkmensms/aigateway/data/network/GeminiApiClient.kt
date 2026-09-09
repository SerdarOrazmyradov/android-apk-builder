package io.turkmensms.aigateway.data.network

@Deprecated("Removed; SMS-Gmail bridge does not use Gemini")
class GeminiApiClient(@Suppress("UNUSED_PARAMETER") apiKey: String) {
    fun sendMessage(message: String): Result<String> =
        Result.failure(UnsupportedOperationException("Gemini client removed"))
}
