package com.example.sampleapp

data class UserConfig(
    val name: String,
    val phoneNumber: String, // Diňe sany saklaň, mysal üçin: 99361358756
    val apiKey: String,
    val model: String = "gemini-2.5-flash" // Bellenen modeller: ["gemini-2.5-flash", "gemini-2.5-pro"]
)

object AllowedUsers {
    // Siz oz API key-leriniz bilen calysyn
    private val users = listOf(
        UserConfig("Serdar", "99361358756", "YOUR_GEMINI_API_KEY_SERDAR"),
        UserConfig("Maral", "99371808643", "YOUR_GEMINI_API_KEY_MARAL"),
        UserConfig("Ejem", "99365263069", "YOUR_GEMINI_API_KEY_EJEM"),
        UserConfig("Jennet", "99361282375", "YOUR_GEMINI_API_KEY_JENNET")
    )

    fun getUser(rawPhoneNumber: String): UserConfig? {
        val cleanNumber = rawPhoneNumber.replace(Regex("[^0-9]"), "")
        return users.find { user ->
            cleanNumber.endsWith(user.phoneNumber) || user.phoneNumber.endsWith(cleanNumber)
        }
    }
}