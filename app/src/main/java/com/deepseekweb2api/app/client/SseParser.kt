package com.deepseekweb2api.app.client

/**
 * Minimal SSE line parser matching the upstream DeepSeek SSE framing:
 *   event: <name>
 *   data: <json>
 * Blank lines terminate a message.
 */
data class SseEvent(val event: String, val data: String)

fun parseSse(text: String): List<SseEvent> {
    val events = mutableListOf<SseEvent>()
    var currentEvent = ""
    for (line in text.lines()) {
        when {
            line.startsWith("event: ") -> currentEvent = line.substring(7)
            line.startsWith("data: ") -> {
                val dataStr = line.substring(6)
                if (dataStr.isNotEmpty()) {
                    events.add(SseEvent(currentEvent, dataStr))
                }
                currentEvent = ""
            }
            line.isBlank() -> currentEvent = ""
        }
    }
    return events
}
