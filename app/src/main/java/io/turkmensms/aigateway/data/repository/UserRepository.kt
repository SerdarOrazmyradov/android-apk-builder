package io.turkmensms.aigateway.data.repository

import android.content.Context
import io.turkmensms.aigateway.data.model.AllowedUser

@Deprecated("Replaced by BridgeConfigStore")
class UserRepository(@Suppress("UNUSED_PARAMETER") context: Context) {
    fun getGeminiApiKey(): String? = null
    fun saveGeminiApiKey(apiKey: String) {}
    fun addUser(user: AllowedUser): Boolean = false
    fun removeUser(phoneNumber: String): Boolean = false
    fun getUser(phoneNumber: String): AllowedUser? = null
    fun getAllUsers(): List<AllowedUser> = emptyList()
    fun isUserAllowed(phoneNumber: String): Boolean = false
    fun getApiKeyForUser(phoneNumber: String): String? = null
}
