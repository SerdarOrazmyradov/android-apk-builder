package com.example.sampleapp

data class UserConfig(
    val name: String,
    val phoneNumber: String, // Diňe sanlar saklanýar
    val apiKey: String,
    val model: String = "gemini-2.5-flash"
)

object AllowedUsers {
    private val users = listOf(
        UserConfig("Serdar", "99361358756", BuildConfig.SERDAR_GEMINI_KEY),
        UserConfig("Maral", "99371808643", BuildConfig.MARAL_GEMINI_KEY),
        UserConfig("Ejem", "99365263069", BuildConfig.EJEM_GEMINI_KEY),
        UserConfig("Jennet", "99361282375", BuildConfig.JENNET_GEMINI_KEY)
    )

    fun getUser(rawPhoneNumber: String): UserConfig? {
        val cleanNumber = rawPhoneNumber.replace(Regex("[^0-9]"), "")
        return users.find { user ->
            cleanNumber.endsWith(user.phoneNumber) || user.phoneNumber.endsWith(cleanNumber)
        }
    }
}