package com.deepseekweb2api.app.client

import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

/**
 * Real DeepSeek connection test using GitHub Secrets.
 * Only runs when deepseek_token and deepseek_cookies env vars are available.
 * Does NOT use Mock - tests real connection to DeepSeek Web.
 */
class DeepSeekRealConnectionTest {

    private val token = System.getenv("deepseek_token")
    private val cookies = System.getenv("deepseek_cookies")

    @Test
    fun testSecretsAvailable() {
        // Verify secrets are accessible (Boolean + length only, no values printed)
        println("deepseek_token available: ${token != null && token.isNotBlank()}")
        println("deepseek_token length: ${token?.length ?: 0}")
        println("deepseek_cookies available: ${cookies != null && cookies.isNotBlank()}")
        println("deepseek_cookies length: ${cookies?.length ?: 0}")

        // Skip if secrets not available
        assumeTrue("deepseek_token not available", token != null && token.isNotBlank())
        assumeTrue("deepseek_cookies not available", cookies != null && cookies.isNotBlank())
    }

    @Test
    fun testRealConnection() = runBlocking {
        assumeTrue("deepseek_token not available", token != null && token.isNotBlank())
        assumeTrue("deepseek_cookies not available", cookies != null && cookies.isNotBlank())

        val client = DeepSeekClient.create(token!!, cookies)
        val result = client.testConnection()

        // Log results (redacted - no token/cookie values)
        println("HTTP code: ${result.httpCode}")
        println("HTTP meaning: ${result.httpCodeMeaning}")
        println("Exception: ${result.exceptionName ?: "none"}")
        println("Is HTML response: ${result.isHtmlResponse}")
        println("Body preview (redacted): ${result.bodyPreview}")

        if (result.httpCode == 401) {
            println("AUTH_ERROR: 401 Unauthorized - Token may be expired or invalid")
        } else if (result.httpCode == 403) {
            println("AUTH_ERROR: 403 Forbidden - Cookies may not match or account blocked")
        } else if (result.httpCode == 429) {
            println("RATE_LIMIT: 429 - Too many requests")
        }

        assertTrue("Connection should succeed (2xx)", result.success)
    }

    @Test
    fun testRealSessionCreation() = runBlocking {
        assumeTrue("deepseek_token not available", token != null && token.isNotBlank())
        assumeTrue("deepseek_cookies not available", cookies != null && cookies.isNotBlank())

        val client = DeepSeekClient.create(token!!, cookies)
        try {
            val sessionId = client.createSession()
            println("Session created successfully: ${sessionId.take(8)}...")
            assertTrue("Session ID should not be empty", sessionId.isNotBlank())
        } catch (e: Exception) {
            println("Session creation failed: ${e.javaClass.simpleName}: ${e.message}")
            throw e
        }
    }

    @Test
    fun testRealChatNonStreaming() = runBlocking {
        assumeTrue("deepseek_token not available", token != null && token.isNotBlank())
        assumeTrue("deepseek_cookies not available", cookies != null && cookies.isNotBlank())

        val client = DeepSeekClient.create(token!!, cookies)
        val sessionId = client.createSession()
        val startTime = System.currentTimeMillis()
        val result = client.chat(
            sessionId = sessionId,
            prompt = "Say 'hello' in one word.",
            modelType = "deepseek-chat",
            thinkingEnabled = false,
            searchEnabled = false,
            parentMessageId = null
        )
        val elapsed = System.currentTimeMillis() - startTime

        println("Non-streaming chat completed in ${elapsed}ms")
        println("Response length: ${result.content.length}")
        println("Has reasoning: ${result.reasoning.isNotEmpty()}")

        assertTrue("Response should not be empty", result.content.isNotBlank())
    }

    @Test
    fun testRealChatStreaming() = runBlocking {
        assumeTrue("deepseek_token not available", token != null && token.isNotBlank())
        assumeTrue("deepseek_cookies not available", cookies != null && cookies.isNotBlank())

        val client = DeepSeekClient.create(token!!, cookies)
        val sessionId = client.createSession()

        val chunkTimes = mutableListOf<Long>()
        val startTime = System.currentTimeMillis()
        var firstChunkTime: Long? = null
        var chunkCount = 0

        client.chatStream(
            sessionId = sessionId,
            prompt = "Count from 1 to 5.",
            modelType = "deepseek-chat",
            thinkingEnabled = false,
            searchEnabled = false,
            parentMessageId = null
        ).collect { event ->
            when (event) {
                is DeepSeekClient.ChatEvent.Text -> {
                    if (firstChunkTime == null) firstChunkTime = System.currentTimeMillis()
                    chunkCount++
                    chunkTimes.add(System.currentTimeMillis() - startTime)
                    println("Chunk $chunkCount received at ${chunkTimes.last()}ms: ${event.text.take(20)}")
                }
                is DeepSeekClient.ChatEvent.Reasoning -> {
                    if (firstChunkTime == null) firstChunkTime = System.currentTimeMillis()
                    chunkCount++
                }
                DeepSeekClient.ChatEvent.Done -> {
                    println("Stream completed: $chunkCount chunks, TTFT=${firstChunkTime?.let { it - startTime }}ms, total=${System.currentTimeMillis() - startTime}ms")
                }
                is DeepSeekClient.ChatEvent.Error -> {
                    println("Stream error: ${event.message}")
                }
            }
        }

        assertTrue("Should receive at least one chunk", chunkCount > 0)
        if (chunkTimes.size >= 2) {
            assertTrue("Chunks should arrive progressively", chunkTimes[0] < chunkTimes[1])
        }
    }
}
