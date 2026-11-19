package com.deepseekweb2api.app.client

import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * DeepSeek Web protocol client.
 *
 * Reimplements the upstream adapter behavior over OkHttp with zero WebView,
 * zero Python, and zero Node.js:
 *   - Authorization + Cookie headers from manually-supplied credentials
 *   - Proof-of-Work challenge / solve (DeepSeekHashV1)
 *   - Session creation and continuation (parent_message_id)
 *   - Streaming and non-streaming chat completion
 *   - DeepThink (reasoning) and Web Search flags
 *
 * Endpoints and payloads are copied verbatim from the upstream adapter:
 *   POST https://chat.deepseek.com/api/v0/chat/create_pow_challenge
 *   POST https://chat.deepseek.com/api/v0/chat_session/create
 *   POST https://chat.deepseek.com/api/v0/chat/completion
 */
class DeepSeekClient private constructor(
    private val client: OkHttpClient,
    private val gson: Gson,
    private val token: String,
    private val cookies: String?,
    private val powSolver: PowSolver,
    private val userAgent: String = DEFAULT_USER_AGENT,
    private val secChUa: String = DEFAULT_SEC_CH_UA
) {
    companion object {
        const val BASE_URL = "https://chat.deepseek.com"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        const val DEFAULT_SEC_CH_UA = "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\""

        fun create(
            token: String,
            cookies: String? = null,
            userAgent: String = DEFAULT_USER_AGENT,
            secChUa: String = DEFAULT_SEC_CH_UA
        ): DeepSeekClient =
            DeepSeekClient(
                OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build(),
                Gson(),
                token,
                cookies,
                PowSolver(),
                userAgent,
                secChUa
            )
    }

    private val authHeader: String
        get() = if (token.startsWith("[redacted]", ignoreCase = true)) "[REDACTED]" else "Bearer $token"

    /** Result of a manual connectivity test against create_pow_challenge. */
    data class ConnectionTestResult(
        val success: Boolean,
        val httpCode: Int? = null,
        val httpCodeMeaning: String? = null,
        val exceptionName: String? = null,
        val exceptionMessage: String? = null,
        val isHtmlResponse: Boolean = false,
        val bodyPreview: String? = null,
        val bodyFull: String? = null,
        val contentType: String? = null
    )

    private fun meaningOf(code: Int): String = when (code) {
        200 -> "نجاح - تم قبول الطلب"
        201 -> "تم الإنشاء بنجاح"
        202 -> "تم قبول الطلب للمعالجة"
        400 -> "طلب خاطئ - تحقق من البيانات المرسلة"
        401 -> "غير مصرح به - التوكن غير صالح أو منتهي الصلاحية"
        403 -> "محظور - لا تملك حق الوصول لهذا المورد"
        429 -> "كثير من الطلبات - تم تقييد المعدل (Rate Limit)"
        500 -> "خطأ في الخادم الداخلي"
        502 -> "بوابة خاطئة - الخادم غير متاح"
        503 -> "الخدمة غير متاحة مؤقتاً"
        else -> if (code in 200..299) "نجاح (كود $code)" else if (code in 400..499) "خطأ من جانب العميل (كود $code)" else if (code in 500..599) "خطأ من جانب الخادم (كود $code)" else "كود استجابة غير معروف (كود $code)"
    }

    private fun redact(text: String): String {
        var result = text
        if (token.isNotBlank() && token.length > 5) {
            result = result.replace(token, "[مخفي]")
        }
        cookies?.let { c ->
            if (c.isNotBlank() && c.length > 5) {
                result = result.replace(c, "[مخفي]")
            }
        }
        result = result.replace(Regex("""Authorization\s*:\s*\S+"""), "Authorization: [مخفي]")
        result = result.replace(Regex("""Cookie\s*:\s*\S+"""), "Cookie: [مخفي]")
        return result
    }

    suspend fun testConnection(): ConnectionTestResult {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/api/v0/chat/create_pow_challenge")
                    .post(jsonBody(mapOf("target_path" to "/api/v0/chat/completion")))
                    .headers(baseHeaders("/api/v0/chat/completion").build())
                    .build()

                client.newCall(request).execute().use { response ->
                    val code = response.code
                    val body = response.body?.string() ?: ""
                    val contentType = response.header("Content-Type") ?: ""
                    val isHtml = contentType.contains("text/html", ignoreCase = true) || body.trimStart().startsWith("<")
                    val redactedBody = redact(body)

                    ConnectionTestResult(
                        success = response.isSuccessful,
                        httpCode = code,
                        httpCodeMeaning = meaningOf(code),
                        isHtmlResponse = isHtml,
                        bodyPreview = redactedBody.take(200),
                        bodyFull = redactedBody.take(1000),
                        contentType = contentType
                    )
                }
            } catch (e: Exception) {
                ConnectionTestResult(
                    success = false,
                    exceptionName = e.javaClass.simpleName,
                    exceptionMessage = e.message ?: e.toString()
                )
            }
        }
    }

    private fun jsonBody(map: Map<String, Any?>): okhttp3.RequestBody =
        gson.toJson(map).toRequestBody(JSON_MEDIA_TYPE)

    /** Base headers captured from a real chat.deepseek.com session (no PoW). */
    private fun baseHeaders(path: String): Headers.Builder {
        val b = Headers.Builder()
            .add("Authorization", authHeader)
        cookies?.let { b.add("Cookie", it) }
        b.add("User-Agent", userAgent)
            .add("Accept", "*/*")
            .add("Accept-Encoding", "gzip, deflate, br, zstd")
            .add("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7")
            .add("Content-Type", "application/json")
            .add("Origin", "https://chat.deepseek.com")
            .add("Referer", "https://chat.deepseek.com/")
            .add("X-Client-Version", "2.3.0")
            .add("X-Client-Platform", "web")
            .add("X-Client-Locale", "zh_CN")
            .add("X-Client-Timezone-Offset", "28800")
            .add("x-client-bundle-id", "com.deepseek.chat")
            .add("Priority", "u=1, i")
            .add("Sec-Ch-Ua", secChUa)
            .add("Sec-Ch-Ua-Mobile", "?0")
            .add("Sec-Ch-Ua-Platform", "\"Windows\"")
            .add("Sec-Fetch-Dest", "empty")
            .add("Sec-Fetch-Mode", "cors")
            .add("Sec-Fetch-Site", "same-origin")
        return b
    }

    /** Headers including a freshly solved Proof-of-Work for the given target path. */
    private suspend fun powHeaders(path: String): Headers {
        val challenge = fetchPowChallenge(path)
        val solution = powSolver.solve(challenge)
        return baseHeaders(path)
            .add("X-DS-PoW-Response", solution.headerValue)
            .build()
    }

    private suspend fun fetchPowChallenge(path: String): PowSolver.Challenge {
        val request = Request.Builder()
            .url("$BASE_URL/api/v0/chat/create_pow_challenge")
            .post(jsonBody(mapOf("target_path" to path)))
            .headers(baseHeaders(path).build())
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("pow challenge failed: ${response.code}")
                val text = response.body?.string() ?: throw IOException("empty pow challenge body")
                val obj = JsonParser.parseString(text).asJsonObject
                val biz = obj.getAsJsonObject("data").getAsJsonObject("biz_data")
                PowSolver.Challenge(
                    challenge = biz.get("challenge").asString,
                    salt = biz.get("salt").asString,
                    expireAt = biz.get("expire_at").asLong,
                    difficulty = biz.get("difficulty").asInt,
                    signature = biz.get("signature").asString,
                    targetPath = biz.get("target_path").asString
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // Session
    // ------------------------------------------------------------------

    suspend fun createSession(): String {
        val headers = powHeaders("/api/v0/chat/completion")
        val request = Request.Builder()
            .url("$BASE_URL/api/v0/chat_session/create")
            .post(jsonBody(mapOf("target_path" to "/api/v0/chat/completion")))
            .headers(headers)
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("create_session failed: ${response.code}")
                val text = response.body?.string() ?: throw IOException("empty session body")
                val obj = JsonParser.parseString(text).asJsonObject
                val biz = obj.getAsJsonObject("data").getAsJsonObject("biz_data")
                biz.get("id")?.takeIf { !it.isJsonNull }?.asString
                    ?: biz.getAsJsonObject("chat_session").get("id").asString
            }
        }
    }

    // ------------------------------------------------------------------
    // Non-streaming completion
    // ------------------------------------------------------------------

    data class ChatResult(val content: String, val reasoning: String)

    suspend fun chat(
        sessionId: String,
        prompt: String,
        modelType: String?,
        thinkingEnabled: Boolean,
        searchEnabled: Boolean,
        parentMessageId: Int?
    ): ChatResult {
        val headers = powHeaders("/api/v0/chat/completion")
        val body = gson.toJson(mapOf(
            "chat_session_id" to sessionId,
            "parent_message_id" to parentMessageId,
            "model_type" to modelType,
            "prompt" to prompt,
            "ref_file_ids" to emptyList<String>(),
            "stream" to false,
            "thinking_enabled" to thinkingEnabled,
            "search_enabled" to searchEnabled,
            "preempt" to false
        ))
        val request = Request.Builder()
            .url("$BASE_URL/api/v0/chat/completion")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .headers(headers)
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("chat failed: ${response.code}")
                val text = response.body?.string() ?: ""
                parseEventsToResult(parseSse(text))
            }
        }
    }

    // ------------------------------------------------------------------
    // Streaming completion
    // ------------------------------------------------------------------

    sealed class ChatEvent {
        object Done : ChatEvent()
        data class Text(val text: String) : ChatEvent()
        data class Reasoning(val text: String) : ChatEvent()
        data class Error(val message: String) : ChatEvent()
    }

    fun chatStream(
        sessionId: String,
        prompt: String,
        modelType: String?,
        thinkingEnabled: Boolean,
        searchEnabled: Boolean,
        parentMessageId: Int?
    ): Flow<ChatEvent> = callbackFlow {
        val headers = withContext(Dispatchers.IO) { powHeaders("/api/v0/chat/completion") }
        val body = gson.toJson(mapOf(
            "chat_session_id" to sessionId,
            "parent_message_id" to parentMessageId,
            "model_type" to modelType,
            "prompt" to prompt,
            "ref_file_ids" to emptyList<String>(),
            "stream" to true,
            "thinking_enabled" to thinkingEnabled,
            "search_enabled" to searchEnabled,
            "preempt" to false
        ))
        val request = Request.Builder()
            .url("$BASE_URL/api/v0/chat/completion")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .headers(headers)
            .build()

        val listener = object : EventSourceListener() {
            private var fragType: String? = null

            override fun onOpen(eventSource: EventSource, response: Response) {}

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val obj = JsonParser.parseString(data).asJsonObject
                    val p = obj.get("p")?.takeIf { !it.isJsonNull }?.asString ?: ""
                    val o = obj.get("o")?.takeIf { !it.isJsonNull }?.asString ?: ""
                    val v = obj.get("v")

                    if (v != null && v.isJsonObject && v.asJsonObject.has("response")) {
                        val resp = v.asJsonObject.getAsJsonObject("response")
                        val fragments = resp.getAsJsonArray("fragments")
                        if (fragments != null && fragments.size() > 0) {
                            val frag = fragments.get(0).asJsonObject
                            fragType = if (frag.get("type").asString == "THINK") "thinking" else "content"
                            val fc = frag.get("content")?.takeIf { !it.isJsonNull }?.asString ?: ""
                            if (fc.isNotEmpty()) {
                                trySend(if (fragType == "thinking") ChatEvent.Reasoning(fc) else ChatEvent.Text(fc))
                            }
                        }
                        return
                    }
                    if (p == "response/fragments/-1/content" && o == "APPEND" && v != null) {
                        val piece = v.asString
                        trySend(if (fragType == "thinking") ChatEvent.Reasoning(piece) else ChatEvent.Text(piece))
                        return
                    }
                    if (p == "response/fragments" && o == "APPEND" && v != null && v.isJsonArray) {
                        val arr = v.asJsonArray
                        if (arr.size() > 0) {
                            val frag = arr.get(0).asJsonObject
                            val nt = frag.get("type").asString
                            fragType = if (nt == "THINK") "thinking" else "content"
                            val seed = frag.get("content")?.takeIf { !it.isJsonNull }?.asString ?: ""
                            if (seed.isNotEmpty()) {
                                trySend(if (fragType == "thinking") ChatEvent.Reasoning(seed) else ChatEvent.Text(seed))
                            }
                        }
                        return
                    }
                    if (p == "response/content" && o == "APPEND" && v != null) {
                        trySend(ChatEvent.Text(v.asString))
                        return
                    }
                    if (v != null && v.isJsonPrimitive && p.isEmpty() && o.isEmpty()) {
                        val piece = v.asString
                        if (piece.isNotEmpty()) {
                            trySend(if (fragType == "thinking") ChatEvent.Reasoning(piece) else ChatEvent.Text(piece))
                        }
                    }
                } catch (e: Exception) {
                    // ignore malformed frames
                }
            }

            override fun onClosed(eventSource: EventSource) {
                trySend(ChatEvent.Done)
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                trySend(ChatEvent.Error(t?.message ?: "stream failed"))
                close()
            }
        }

        val eventSource = EventSources.createFactory(client).newEventSource(request, listener)
        awaitClose { eventSource.cancel() }
    }

    // ------------------------------------------------------------------
    // SSE -> result (shared with non-stream path)
    // ------------------------------------------------------------------

    private fun parseEventsToResult(events: List<SseEvent>): ChatResult {
        val contentParts = mutableListOf<String>()
        val thinkingParts = mutableListOf<String>()
        var fragType: String? = null
        for (sse in events) {
            try {
                val obj = JsonParser.parseString(sse.data).asJsonObject
                val p = obj.get("p")?.takeIf { !it.isJsonNull }?.asString ?: ""
                val o = obj.get("o")?.takeIf { !it.isJsonNull }?.asString ?: ""
                val v = obj.get("v")

                if (v != null && v.isJsonObject && v.asJsonObject.has("response")) {
                    val resp = v.asJsonObject.getAsJsonObject("response")
                    val fragments = resp.getAsJsonArray("fragments")
                    if (fragments != null && fragments.size() > 0) {
                        val frag = fragments.get(0).asJsonObject
                        fragType = if (frag.get("type").asString == "THINK") "thinking" else "content"
                        val fc = frag.get("content")?.takeIf { !it.isJsonNull }?.asString ?: ""
                        if (fc.isNotEmpty()) {
                            if (fragType == "thinking") thinkingParts.add(fc) else contentParts.add(fc)
                        }
                        continue
                    }
                }
                if (p == "response/fragments/-1/content" && o == "APPEND" && v != null) {
                    if (fragType == "thinking") thinkingParts.add(v.asString) else contentParts.add(v.asString)
                    continue
                }
                if (p == "response/fragments" && o == "APPEND" && v != null && v.isJsonArray) {
                    val arr = v.asJsonArray
                    if (arr.size() > 0) {
                        val frag = arr.get(0).asJsonObject
                        val nt = frag.get("type").asString
                        fragType = if (nt == "THINK") "thinking" else "content"
                        val seed = frag.get("content")?.takeIf { !it.isJsonNull }?.asString ?: ""
                        if (seed.isNotEmpty()) {
                            if (fragType == "thinking") thinkingParts.add(seed) else contentParts.add(seed)
                        }
                    }
                    continue
                }
                if (p == "response/content" && o == "APPEND" && v != null) {
                    contentParts.add(v.asString)
                    continue
                }
                if (v != null && v.isJsonPrimitive && p.isEmpty() && o.isEmpty()) {
                    val token = v.asString
                    if (token.isNotEmpty()) {
                        if (fragType == "thinking") thinkingParts.add(token) else contentParts.add(token)
                    }
                }
            } catch (e: Exception) {
                // ignore malformed frames
            }
        }
        return ChatResult(content = contentParts.joinToString(""), reasoning = thinkingParts.joinToString(""))
    }
}
