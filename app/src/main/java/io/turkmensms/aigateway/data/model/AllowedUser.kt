package io.turkmensms.aigateway.data.model

import com.google.gson.Gson

data class AllowedUser(
    val phoneNumber: String,
    val geminiApiKey: String,
    val addedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): AllowedUser? = try {
            Gson().fromJson(json, AllowedUser::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
