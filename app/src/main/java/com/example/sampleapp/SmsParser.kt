package com.example.sampleapp

object SmsParser {
    fun extractPhoneNumber(subject: String): String? {
        // Ýaýyň ( ) içindäki +993 bilen başlaýan nomeri alýar
        val regex = Regex("""\(\+?(\d+)\)""")
        val matchResult = regex.find(subject)
        return matchResult?.groupValues?.get(1)?.let { "+$it" }
    }
}