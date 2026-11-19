package com.deepseekweb2api.app.client

import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

/**
 * Real DeepSeek connection test using GitHub Secrets.
 * Only runs when deepseek_token and deepseek_cookies env vars are available.
 * Does NOT use Mock - tests real connection to DeepSeek Web.
 * Each test has independent timeout to prevent blocking other tests.
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

    @Test(timeout = 30000)
    fun testRealConnection() = runBlocking {
        assumeTrue("deepseek_token not available", token != null && token.isNotBlank())
        assumeTrue("deepseek_cookies not available", cookies != null && cookies.isNotBlank())

        println("[testRealConnection] Starting connection test...")
        val client = DeepSeekClient.create(token!!, cookies)
        val result = withTimeoutOrNull(25_000) { client.testConnection() }

        if (result == null) {
            println("[testRealConnection] TIMEOUT after 25s")
            fail("Connection test timed out after 25 seconds")
        }

        // Log results (redacted - no token/cookie values)
        println("[testRealConnection] HTTP code: ${result.httpCode}")
        println("[testRealConnection] HTTP meaning: ${result.httpCodeMeaning}")
        println("[testRealConnection] Exception: ${result.exceptionName ?: "none"}")
        println("[testRealConnection] Is HTML response: ${result.isHtmlResponse}")
        println("[testRealConnection] Body preview (redacted): ${result.bodyPreview}")

        if (result.httpCode == 401) {
            println("AUTH_ERROR: 401 Unauthorized - Token may be expired or invalid")
        } else if (result.httpCode == 403) {
            println("AUTH_ERROR: 403 Forbidden - Cookies may not match or account blocked")
        } else if (result.httpCode == 429) {
            println("RATE_LIMIT: 429 - Too many requests")
        }

        assertTrue("Connection should succeed (2xx)", result.success)
        println("[testRealConnection] COMPLETED successfully")
    }

    @Test(timeout = 45000)
    fun testRealSessionCreation() = runBlocking {
        assumeTrue("deepseek_token not available", token != null && token.isNotBlank())
        assumeTrue("deepseek_cookies not available", cookies != null && cookies.isNotBlank())

        println("[testRealSessionCreation] Starting session creation...")
        val client = DeepSeekClient.create(token!!, cookies)
        val sessionId = withTimeoutOrNull(40_000) { client.createSession() }

        if (sessionId == null) {
            println("[testRealSessionCreation] TIMEOUT after 40s")
            fail("Session creation timed out after 40 seconds")
        }

        println("[testRealSessionCreation] Session created successfully: ${sessionId.take(8)}...")
        assertTrue("Session ID should not be empty", sessionId.isNotBlank())
        println("[testRealSessionCreation] COMPLETED successfully")
    }

    @Test(timeout = 60000)
    fun testRealChatNonStreaming() = runBlocking {
        assumeTrue("deepseek_token not available", token != null && token.isNotBlank())
        assumeTrue("deepseek_cookies not available", cookies != null && cookies.isNotBlank())

        println("[testRealChatNonStreaming] Starting non-streaming chat...")
        val client = DeepSeekClient.create(token!!, cookies)
        val sessionId = withTimeoutOrNull(40_000) { client.createSession() }

        if (sessionId == null) {
            println("[testRealChatNonStreaming] Session creation TIMEOUT")
            fail("Session creation timed out")
        }

        val startTime = System.currentTimeMillis()
        val result = withTimeoutOrNull(50_000) {
            client.chat(
                sessionId = sessionId,
                prompt = "Say 'hello' in one word.",
                modelType = "deepseek-chat",
                thinkingEnabled = false,
                searchEnabled = false,
                parentMessageId = null
            )
        }
        val elapsed = System.currentTimeMillis() - startTime

        if (result == null) {
            println("[testRealChatNonStreaming] TIMEOUT after 50s")
            fail("Non-streaming chat timed out after 50 seconds")
        }

        println("[testRealChatNonStreaming] Completed in ${elapsed}ms")
        println("[testRealChatNonStreaming] Response length: ${result.content.length}")
        println("[testRealChatNonStreaming] Has reasoning: ${result.reasoning.isNotEmpty()}")

        assertTrue("Response should not be empty", result.content.isNotBlank())
        println("[testRealChatNonStreaming] COMPLETED successfully")
    }

    @Test(timeout = 90000)
    fun testRealChatStreaming() = runBlocking {
        assumeTrue("deepseek_token not available", token != null && token.isNotBlank())
        assumeTrue("deepseek_cookies not available", cookies != null && cookies.isNotBlank())

        println("[testRealChatStreaming] Starting streaming chat...")
        val client = DeepSeekClient.create(token!!, cookies)
        val sessionId = withTimeoutOrNull(40_000) { client.createSession() }

        if (sessionId == null) {
            println("[testRealChatStreaming] Session creation TIMEOUT")
            fail("Session creation timed out")
        }

        val chunkTimes = mutableListOf<Long>()
        val startTime = System.currentTimeMillis()
        var firstChunkTime: Long? = null
        var chunkCount = 0
        var streamCompleted = false

        val streamResult = withTimeoutOrNull(80_000) {
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
                        println("[testRealChatStreaming] Chunk $chunkCount received at ${chunkTimes.last()}ms: ${event.text.take(20)}")
                    }
                    is DeepSeekClient.ChatEvent.Reasoning -> {
                        if (firstChunkTime == null) firstChunkTime = System.currentTimeMillis()
                        chunkCount++
                    }
                    DeepSeekClient.ChatEvent.Done -> {
                        streamCompleted = true
                        println("[testRealChatStreaming] Stream completed: $chunkCount chunks, TTFT=${firstChunkTime?.let { it - startTime }}ms, total=${System.currentTimeMillis() - startTime}ms")
                    }
                    is DeepSeekClient.ChatEvent.Error -> {
                        println("[testRealChatStreaming] Stream error: ${event.message}")
                    }
                }
            }
        }

        if (streamResult == null) {
            println("[testRealChatStreaming] TIMEOUT after 80s")
            fail("Streaming timed out after 80 seconds")
        }

        if (!streamCompleted) {
            println("[testRealChatStreaming] Stream did not complete (no Done event)")
            fail("Stream did not complete")
        }

        assertTrue("Should receive at least one chunk", chunkCount > 0)
        if (chunkTimes.size >= 2) {
            assertTrue("Chunks should arrive progressively", chunkTimes[0] < chunkTimes[1])
        }
        println("[testRealChatStreaming] COMPLETED successfully")
    }
}
