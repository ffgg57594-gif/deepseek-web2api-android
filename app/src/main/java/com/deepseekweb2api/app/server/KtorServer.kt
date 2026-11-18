package com.deepseekweb2api.app.server

import com.deepseekweb2api.app.client.ChatEvent
import com.deepseekweb2api.app.client.DeepSeekClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.contentNegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.netty.*
import kotlinx.coroutines.channels.consumeEach
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
 * Streams SSE when `stream: true`, otherwise returns a single JSON completion.
 * Supports DeepThink (reasoning) and Web Search flags.
 */
class KtorServer(
    private val port: Int,
    private val deepSeekClient: DeepSeekClient? = null
) {
    private var server: ApplicationServer? = null

    fun start() {
        server = embeddedServer(
            Netty,
            host = "127.0.0.1",
            port = port,
            module = Application::serverModule
        )
        server?.start(wait = false)
    }

    fun stop() {
        server?.stop()
        server = null
    }

    private fun Application.serverModule() {
        install(ContentNegotiation) {
            json()
        }
        routing {
            get("/health") {
                call.respond(mapOf("status" to "ok", "server" to "deepseek-web2api-local"))
            }

            get("/v1/models") {
                call.respond(mapOf(
                    "object" to "list",
                    "data" to listOf(
                        mapOf("id" to "deepseek-chat", "object" to "model", "owned_by" to "deepseek"),
                        mapOf("id" to "deepseek-reasoner", "object" to "model", "owned_by" to "deepseek")
                    )
                ))
            }

            post("/v1/chat/completions") {
                val client = deepSeekClient ?: run {
                    call.respond(mapOf("error" to "no DeepSeek account configured"))
                    return@post
                }
                val body = call.receive<Map<String, Any?>>()
                val messages = body["messages"] as? List<Map<String, Any?>> ?: emptyList()
                val stream = body["stream"] as? Boolean ?: false
                val model = body["model"] as? String
                val thinkingEnabled = body["thinking_enabled"] as? Boolean ?: false
                val searchEnabled = body["search_enabled"] as? Boolean ?: false

                val sessionId = client.createSession()
                val prompt = messages
                    .map { (it["role"] as? String) + ": " + (it["content"] as? String ?: "") }
                    .joinToString("\n")

                if (stream) {
                    call.response.status(HttpStatusCode.OK)
                    call.response.contentType(ContentType.Text.EventStream)
                    launch {
                        client.chatStream(
                            sessionId = sessionId,
                            prompt = prompt,
                            modelType = model,
                            thinkingEnabled = thinkingEnabled,
                            searchEnabled = searchEnabled,
                            parentMessageId = null
                        ).collect { event ->
                            when (event) {
                                is ChatEvent.Text -> {
                                    val data = mapOf(
                                        "choices" to listOf(mapOf(
                                            "delta" to mapOf("content" to event.text)
                                        ))
                                    )
                                    call.response.channel.writeText("data: ${gson.toJson(data)}\n\n")
                                    call.response.channel.flush()
                                }
                                is ChatEvent.Reasoning -> {
                                    val data = mapOf(
                                        "choices" to listOf(mapOf(
                                            "delta" to mapOf("content" to "[DEEPSEEK_REASONING]${event.text}")
                                        ))
                                    )
                                    call.response.channel.writeText("data: ${gson.toJson(data)}\n\n")
                                    call.response.channel.flush()
                                }
                                ChatEvent.Done -> {
                                    call.response.channel.writeText("data: [DONE]\n\n")
                                    call.response.channel.flush()
                                }
                                is ChatEvent.Error -> {
                                    call.response.channel.writeText(
                                        "data: ${gson.toJson(mapOf("error" to event.message))}\n\n"
                                    )
                                    call.response.channel.flush()
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
                    val message = mutableMapOf<String, Any?>("role" to "assistant", "content" to result.content)
                    if (result.reasoning.isNotEmpty()) {
                        message["reasoning_content"] = result.reasoning
                    }
                    val choices = listOf(mapOf(
                        "index" to 0,
                        "message" to message,
                        "finish_reason" to "stop"
                    ))
                    call.respond(mapOf(
                        "id" to "chatcmpl-local",
                        "object" to "chat.completion",
                        "created" to System.currentTimeMillis() / 1000,
                        "model" to model ?: "deepseek-chat",
                        "choices" to choices
                    ))
                }
            }
        }
    }

    private val gson = com.google.gson.Gson()
}
