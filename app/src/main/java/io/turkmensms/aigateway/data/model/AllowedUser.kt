package io.turkmensms.aigateway.data.model

data class AllowedUser(
    val phoneNumber: String,
    val geminiApiKey: String = "",
    val addedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
