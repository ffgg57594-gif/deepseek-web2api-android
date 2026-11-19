package com.deepseekweb2api.app.server

import com.deepseekweb2api.app.client.DeepSeekClient
import com.google.gson.Gson
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.serialization.gson.gson
import kotlinx.coroutines.flow.collect

/**
 * Embedded local HTTP server listening on 127.0.0.1:8080.
 *
 * Endpoints:
 *   GET  /health
 *   GET  /v1/models
 *   POST /v1/chat/completions  — OpenAI-compatible, forwarded to DeepSeek Web client
 *
 * Uses the CIO engine (pure Kotlin coroutines) rather than Netty, whose native
 * transport is not bundled for Android ARM64.
 */
class KtorServer(
    private val port: Int,
    private val deepSeekClient: DeepSeekClient? = null
) {
    internal var server: ApplicationEngine? = null
    private val gson = Gson()

    fun start() {
        server = embeddedServer(CIO, port = port, host = "127.0.0.1") {
            serverModule()
        }
        server?.start(wait = false)
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }

    private fun Application.serverModule() {
        install(ContentNegotiation) {
            gson()
        }
        routing {
            get("/health") {
                call.respond(mapOf("status" to "ok", "server" to "deepseek-web2api-local"))
            }

            get("/v1/models") {
                call.respond(
                    mapOf(
                        "object" to "list",
                        "data" to listOf(
                            mapOf("id" to "deepseek-chat", "object" to "model", "owned_by" to "deepseek"),
                            mapOf("id" to "deepseek-reasoner", "object" to "model", "owned_by" to "deepseek")
                        )
                    )
                )
            }

            post("/v1/chat/completions") {
                val client = deepSeekClient
                if (client == null) {
                    call.respond(mapOf("error" to "no DeepSeek account configured"))
                    return@post
                }

                val body = call.receive<Map<String, Any?>>()
                @Suppress("UNCHECKED_CAST")
                val messages = (body["messages"] as? List<Map<String, Any?>>) ?: emptyList()
                val stream = body["stream"] as? Boolean ?: false
                val model = body["model"] as? String
                val thinkingEnabled = body["thinking_enabled"] as? Boolean ?: false
                val searchEnabled = body["search_enabled"] as? Boolean ?: false

                val requestStart = System.currentTimeMillis()
                val sessionId = client.createSession()
                val prompt = messages.joinToString("\n") { msg ->
                    "${msg["role"]}: ${msg["content"]}"
                }

                if (stream) {
                    call.response.headers.append("Cache-Control", "no-cache")
                    call.response.headers.append("Connection", "keep-alive")
                    call.response.headers.append("X-Accel-Buffering", "no")
                    call.response.status(HttpStatusCode.OK)
                    call.respondTextWriter(ContentType.Text.EventStream) {
                        val streamStart = System.currentTimeMillis()
                        var firstChunkTime: Long? = null
                        var chunkCount = 0
                        client.chatStream(
                            sessionId = sessionId,
                            prompt = prompt,
                            modelType = model,
                            thinkingEnabled = thinkingEnabled,
                            searchEnabled = searchEnabled,
                            parentMessageId = null
                        ).collect { event ->
                            when (event) {
                                is DeepSeekClient.ChatEvent.Text -> {
                                    if (firstChunkTime == null) firstChunkTime = System.currentTimeMillis()
                                    chunkCount++
                                    write(sseDelta(event.text, model))
                                    flush()
                                }
                                is DeepSeekClient.ChatEvent.Reasoning -> {
                                    if (firstChunkTime == null) firstChunkTime = System.currentTimeMillis()
                                    chunkCount++
                                    write(sseDelta("[DEEPSEEK_REASONING]${event.text}", model))
                                    flush()
                                }
                                DeepSeekClient.ChatEvent.Done -> {
                                    write("data: [DONE]\n\n")
                                    flush()
                                    val totalMs = System.currentTimeMillis() - requestStart
                                    val ttftMs = firstChunkTime?.let { it - requestStart } ?: -1
                                    println("STREAM_TIMING: request_start=$requestStart ttft_ms=$ttftMs chunks=$chunkCount total_ms=$totalMs")
                                }
                                is DeepSeekClient.ChatEvent.Error -> {
                                    write("data: ${gson.toJson(mapOf("error" to mapOf("message" to event.message, "type" to "server_error")))}\n\n")
                                    flush()
                                }
                            }
                        }
                    }
                } else {
                    val powEnd = System.currentTimeMillis()
                    val result = client.chat(
                        sessionId = sessionId,
                        prompt = prompt,
                        modelType = model,
                        thinkingEnabled = thinkingEnabled,
                        searchEnabled = searchEnabled,
                        parentMessageId = null
                    )
                    val responseComplete = System.currentTimeMillis()
                    println("NONSTREAM_TIMING: request_start=$requestStart pow_end=$powEnd response_complete=$responseComplete total_ms=${responseComplete - requestStart}")
                    val message = mutableMapOf<String, Any?>(
                        "role" to "assistant",
                        "content" to result.content
                    )
                    if (result.reasoning.isNotEmpty()) {
                        message["reasoning_content"] = result.reasoning
                    }
                    call.respond(
                        mapOf(
                            "id" to "chatcmpl-local",
                            "object" to "chat.completion",
                            "created" to System.currentTimeMillis() / 1000,
                            "model" to (model ?: "deepseek-chat"),
                            "choices" to listOf(
                                mapOf(
                                    "index" to 0,
                                    "message" to message,
                                    "finish_reason" to "stop"
                                )
                            )
                        )
                    )
                }
            }
        }
    }

    private fun sseDelta(content: String, model: String? = null): String {
        val payload = mapOf(
            "id" to "chatcmpl-local",
            "object" to "chat.completion.chunk",
            "created" to System.currentTimeMillis() / 1000,
            "model" to (model ?: "deepseek-chat"),
            "choices" to listOf(
                mapOf("index" to 0, "delta" to mapOf("content" to content), "finish_reason" to null)
            )
        )
        return "data: ${gson.toJson(payload)}\n\n"
    }
}
