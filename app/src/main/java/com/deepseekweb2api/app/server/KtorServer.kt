package com.deepseekweb2api.app.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.contentNegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.netty.*

/**
 * Embedded local HTTP server listening on 127.0.0.1:8080.
 * Endpoints:
 *   GET  /health
 *   GET  /v1/models
 *   POST /v1/chat/completions  (OpenAI-compatible, forwarded to DeepSeek Web client)
 */
class KtorServer(private val port: Int) {
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
                // Forwarded to the DeepSeek Web client in the next implementation step.
                call.respond(mapOf("error" to "chat completions not yet wired to DeepSeek client"))
            }
        }
    }
}
