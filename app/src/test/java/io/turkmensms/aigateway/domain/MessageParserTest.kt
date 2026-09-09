package io.turkmensms.aigateway.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageParserTest {

    @Test
    fun questionExtractsPhoneAndBody() {
        val result = MessageParser.parse("QUESTION +99361111111", "hello there")
        assertTrue(result is InboundIntent.Question)
        val question = result as InboundIntent.Question
        assertEquals("+99361111111", question.phone)
        assertEquals("hello there", question.body)
    }

    @Test
    fun replySubjectExtractsPhone() {
        val result = MessageParser.parse("Re: SMS from +99361111111", "yes")
        assertTrue(result is InboundIntent.SmsReply)
        val reply = result as InboundIntent.SmsReply
        assertEquals("+99361111111", reply.phone)
        assertEquals("yes", reply.body)
    }

    @Test
    fun nestedReplyPrefixesStillMatch() {
        val result = MessageParser.parse("Re: Re: SMS from 99361111111", "ping")
        assertTrue(result is InboundIntent.SmsReply)
        assertEquals("99361111111", (result as InboundIntent.SmsReply).phone)
    }

    @Test
    fun quotedReplyBodyIsStripped() {
        val body = "new text\n\nOn Mon, Bob wrote:\n> old"
        val result = MessageParser.parse("SMS from +99361111111", body) as InboundIntent.SmsReply
        assertEquals("new text", result.body)
    }

    @Test
    fun quotedLinesStartingWithAngleBracketAreStripped() {
        val body = "ok\n> previous"
        val result = MessageParser.parse("SMS from +99361111111", body) as InboundIntent.SmsReply
        assertEquals("ok", result.body)
    }

    @Test
    fun unknownSubjectIsSkipped() {
        assertEquals(InboundIntent.Skip, MessageParser.parse("Hello world", "payload"))
    }

    @Test
    fun missingPhoneOnQuestionIsSkipped() {
        assertEquals(InboundIntent.Skip, MessageParser.parse("QUESTION hi", "x"))
    }

    @Test
    fun normalizeKeepsPlusAndDigits() {
        assertEquals("+99361111111", PhoneNormalizer.normalize("+993-6111-1111"))
        assertEquals("99361111111", PhoneNormalizer.normalize("99361111111"))
        assertNull(PhoneNormalizer.normalize("123"))
        assertNull(PhoneNormalizer.normalize(""))
    }
}
