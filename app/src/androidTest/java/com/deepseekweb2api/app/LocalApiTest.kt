package com.deepseekweb2api.app

import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.deepseekweb2api.app.server.LocalApiService
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * اختبار نقاط نهاية API المحلية dentro المحاكي.
 * يستخدم ADB reverse لتوجيه منفذ 8080 من الجهاز إلى المضيف.
 */
@RunWith(AndroidJUnit4::class)
class LocalApiTest {

    private lateinit var context: Context
    private lateinit var client: OkHttpClient
    private val gson = Gson()

    private const val BASE_URL = "http://127.0.0.1:8080"
    private const val JSON = "application/json".toMediaType()

    @Before
    fun setUp() {
        val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
        context = instrumentation.targetContext
        client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        // بدء خدمة LocalApiService إذا لم تكن تعمل
        val intent = Intent(context, LocalApiService::class.java)
        intent.action = LocalApiService.ACTION_START_SERVER
        context.startForegroundService(intent)
    }

    @After
    fun tearDown() {
        val intent = Intent(context, LocalApiService::class.java)
        intent.action = LocalApiService.ACTION_STOP_SERVER
        context.stopService(intent)
        client.dispatcher.executorService.shutdown()
    }

    private fun get(path: String): Response {
        val request = Request.Builder()
            .url("$BASE_URL$path")
            .get()
            .build()
        return client.newCall(request).execute()
    }

    private fun post(path: String, body: String): Response {
        val request = Request.Builder()
            .url("$BASE_URL$path")
            .post(RequestBody.create(body, JSON))
            .build()
        return client.newCall(request).execute()
    }

    @Test
    fun healthEndpointReturnsOk() {
        val response = get("/health")
        assert(response.isSuccessful) { "Expected 200, got ${response.code}" }
        val body = response.body?.string() ?: ""
        assert(body.contains("\"status\"")) { "Response should contain status field, got: $body" }
    }

    @Test
    fun modelsEndpointReturnsList() {
        val response = get("/v1/models")
        assert(response.isSuccessful) { "Expected 200, got ${response.code}" }
        val body = response.body?.string() ?: ""
        val json = JsonParser.parseString(body).asJsonObject
        val data = json.getAsJsonArray("data")
        assert(data != null && data.size() > 0) { "Models list should not be empty" }
    }

    @Test
    fun chatCompletionsNonStreamingReturnsAnswer() {
        val payload = """
        {
          "model": "deepseek-chat",
          "messages": [{"role": "user", "content": "مرحبا، كم عدد الكواكب في النظام الشمسي؟"}],
          "stream": false,
          "thinking_enabled": false,
          "search_enabled": false
        }
        """.trimIndent()
        val response = post("/v1/chat/completions", payload)
        assert(response.isSuccessful) { "Expected 200, got ${response.code}. Body: ${response.body?.string()}" }
        val body = response.body?.string() ?: ""
        val json = JsonParser.parseString(body).asJsonObject
        val choices = json.getAsJsonArray("choices")
        assert(choices != null && choices.size() > 0) { "Choices should not be empty" }
        val content = choices.get(0).asJsonObject.getAsJsonObject("message").get("content").asString
        assert(content.isNotEmpty()) { "Response content should not be empty" }
    }

    @Test
    fun invalidRequestReturnsError() {
        val response = post("/v1/chat/completions", "not json")
        // قد يكون 400 أو 500 حسب التنفيذ، ولكن يجب ألا يكون 200 ناجحًا مع محتوى خاطئ
        assert(response.code != 200 || response.body?.string().contains("error") == true)
    }
}
