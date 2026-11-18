package com.deepseekweb2api.app.server

import com.deepseekweb2api.app.client.DeepSeekClient
import com.google.gson.Gson
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
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
import kotlinx.coroutines.launch

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
    private var server: ApplicationEngine? = null
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

                val sessionId = client.createSession()
                val prompt = messages.joinToString("\n") { msg ->
                    "${msg["role"]}: ${msg["content"]}"
                }

                if (stream) {
                    call.response.status(HttpStatusCode.OK)
                    launch {
                        call.respondTextWriter(ContentType.Text.EventStream) {
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
                                        write(sseDelta(event.text))
                                        flush()
                                    }
                                    is DeepSeekClient.ChatEvent.Reasoning -> {
                                        write(sseDelta("[DEEPSEEK_REASONING]${event.text}"))
                                        flush()
                                    }
                                    DeepSeekClient.ChatEvent.Done -> {
                                        write("data: [DONE]\n\n")
                                        flush()
                                    }
                                    is DeepSeekClient.ChatEvent.Error -> {
                                        write("data: ${gson.toJson(mapOf("error" to event.message))}\n\n")
                                        flush()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val result = client.chat(
                        sessionId = sessionId,
                        prompt = prompt,
                        modelType = model,
                        thinkingEnabled = thinkingEnabled,
                        searchEnabled = searchEnabled,
                        parentMessageId = null
                    )
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

    private fun sseDelta(content: String): String {
        val payload = mapOf(
            "choices" to listOf(
                mapOf("delta" to mapOf("content" to content))
            )
        )
        return "data: ${gson.toJson(payload)}\n\n"
    }
}
