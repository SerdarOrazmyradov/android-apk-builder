package com.example.sampleapp

data class UserConfig(
    val name: String,
    val phoneNumber: String,
)

object AllowedUsers {
    private val users = listOf(
        UserConfig("Serdar", "99361358756"),
        UserConfig("Maral", "99371808643"),
        UserConfig("Ejem", "99365263069"),
        UserConfig("Jennet", "99361282375")
    )

    fun getUser(rawPhoneNumber: String): UserConfig? {
        val cleanNumber = rawPhoneNumber.replace(Regex("[^0-9]"), "")
        return users.find { user ->
            cleanNumber.endsWith(user.phoneNumber) || user.phoneNumber.endsWith(cleanNumber)
        }
    }
}