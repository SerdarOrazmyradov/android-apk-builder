package io.turkmensms.aigateway.domain

object PhoneNormalizer {
    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val keepPlus = trimmed.startsWith("+")
        val digits = trimmed.filter { it.isDigit() }
        if (digits.length !in 8..15) return null
        return if (keepPlus) "+$digits" else digits
    }
}
