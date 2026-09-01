package io.turkmensms.aigateway.data.repository

import android.content.Context
import android.content.SharedPreferences
import io.turkmensms.aigateway.data.model.AllowedUser

class UserRepository(context: Context) {
    private val sharedPrefs: SharedPreferences = 
        context.getSharedPreferences("ai_gateway_users", Context.MODE_PRIVATE)
    
    private val apiKeyPrefs: SharedPreferences = 
        context.getSharedPreferences("ai_gateway_settings", Context.MODE_PRIVATE)

    companion object {
        private const val USERS_PREFIX = "user_"
        private const val API_KEY = "gemini_api_key"
    }

    // Get main Gemini API Key
    fun getGeminiApiKey(): String? = apiKeyPrefs.getString(API_KEY, null)

    // Save main Gemini API Key
    fun saveGeminiApiKey(apiKey: String) {
        apiKeyPrefs.edit().putString(API_KEY, apiKey).apply()
    }

    // Add or update allowed user
    fun addUser(user: AllowedUser): Boolean {
        return try {
            sharedPrefs.edit()
                .putString(USERS_PREFIX + user.phoneNumber, user.toJson())
                .apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Remove user
    fun removeUser(phoneNumber: String): Boolean {
        return try {
            sharedPrefs.edit()
                .remove(USERS_PREFIX + phoneNumber)
                .apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Get user by phone number
    fun getUser(phoneNumber: String): AllowedUser? {
        val json = sharedPrefs.getString(USERS_PREFIX + phoneNumber, null)
        return json?.let { AllowedUser.fromJson(it) }
    }

    // Get all allowed users
    fun getAllUsers(): List<AllowedUser> {
        return sharedPrefs.all
            .filter { it.key.startsWith(USERS_PREFIX) }
            .mapNotNull { (_, value) -> 
                AllowedUser.fromJson(value as? String ?: return@mapNotNull null)
            }
            .sortedByDescending { it.addedAt }
    }

    // Check if user is allowed
    fun isUserAllowed(phoneNumber: String): Boolean {
        val user = getUser(phoneNumber)
        return user?.isActive == true
    }

    // Get API key for specific user (use their own key if set, fallback to main key)
    fun getApiKeyForUser(phoneNumber: String): String? {
        val user = getUser(phoneNumber)
        return if (!user?.geminiApiKey.isNullOrBlank()) {
            user?.geminiApiKey
        } else {
            getGeminiApiKey()
        }
    }
}
