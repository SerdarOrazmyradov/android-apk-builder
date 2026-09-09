package io.turkmensms.aigateway.domain

sealed class InboundIntent {
    data class Question(val phone: String, val body: String) : InboundIntent()
    data class SmsReply(val phone: String, val body: String) : InboundIntent()
    object Skip : InboundIntent()
}

object MessageParser {
    private val questionRegex =
        Regex("^\\s*QUESTION\\s+(\\+?[0-9][0-9\\s\\-()]{6,20})\\s*$", RegexOption.IGNORE_CASE)
    private val smsFromRegex =
        Regex("^\\s*SMS from\\s+(\\+?[0-9][0-9\\s\\-()]{6,20})\\s*$", RegexOption.IGNORE_CASE)
    private val replyPrefixRegex =
        Regex("^(re|fw|fwd)\\s*:\\s*", RegexOption.IGNORE_CASE)
    private val onWroteRegex =
        Regex("^On .+wrote:\\s*$", RegexOption.IGNORE_CASE)

    fun parse(subject: String?, body: String?): InboundIntent {
        val sub = subject?.trim().orEmpty()
        if (sub.isEmpty()) return InboundIntent.Skip

        val questionMatch = questionRegex.matchEntire(sub)
        if (questionMatch != null) {
            val phone = PhoneNormalizer.normalize(questionMatch.groupValues[1]) ?: return InboundIntent.Skip
            return InboundIntent.Question(phone, body.orEmpty().trim())
        }

        val stripped = stripReplyPrefixes(sub)
        val smsMatch = smsFromRegex.matchEntire(stripped)
        if (smsMatch != null) {
            val phone = PhoneNormalizer.normalize(smsMatch.groupValues[1]) ?: return InboundIntent.Skip
            return InboundIntent.SmsReply(phone, extractNewBody(body.orEmpty()))
        }
        return InboundIntent.Skip
    }

    fun stripReplyPrefixes(subject: String): String {
        var current = subject.trim()
        while (true) {
            val next = current.replaceFirst(replyPrefixRegex, "").trim()
            if (next == current) return current
            current = next
        }
    }

    fun extractNewBody(body: String): String {
        val lines = body.replace("\r\n", "\n").split("\n")
        val cut = lines.indexOfFirst { line ->
            val t = line.trim()
            t.startsWith(">") || onWroteRegex.matches(t)
        }
        val kept = if (cut >= 0) lines.subList(0, cut) else lines
        return kept.joinToString("\n").trim()
    }
}
