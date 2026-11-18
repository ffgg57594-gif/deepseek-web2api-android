package com.deepseekweb2api.app.client

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبار JVM مستقل لـ SseParser.
 * لا يحتاج Android Emulator — يعمل على أي JVM.
 */
class SseParserTest {

    @Test
    fun parseSseShouldExtractEvents() {
        val sseText = """
            |event: message
            |data: {"p":"response/content","o":"APPEND","v":"مرحبا"}
            |
            |event: message
            |data: {"p":"response/content","o":"APPEND","v":"世界"}
            |
            |event: done
            |data: [DONE]
            |""".trimMargin()

        val events = parseSse(sseText)

        assertEquals(3, events.size)
        assertEquals("message", events[0].event)
        assertEquals("{\"p\":\"response/content\",\"o\":\"APPEND\",\"v\":\"مرحبا\"}", events[0].data)
        assertEquals("message", events[1].event)
        assertEquals("done", events[2].event)
        assertEquals("[DONE]", events[2].data)
    }

    @Test
    fun parseSseShouldHandleEmptyInput() {
        val events = parseSse("")
        assertEquals(0, events.size)
    }

    @Test
    fun parseSseShouldIgnoreLinesWithoutData() {
        val sseText = """
            |event: message
            |some random line without data prefix
            |
            |event: message
            |data: actual data
            |""".trimMargin()

        val events = parseSse(sseText)
        assertEquals(1, events.size)
        assertEquals("actual data", events[0].data)
    }
}
