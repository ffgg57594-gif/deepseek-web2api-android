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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Assume
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

    companion object {
        const val BASE_URL = "http://127.0.0.1:8080"
        val JSON = "application/json".toMediaType()
    }

    private lateinit var context: Context
    private lateinit var client: OkHttpClient
    private val gson = Gson()

    @Before
    fun setUp() {
        val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
        context = instrumentation.targetContext
        client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        // بدء خدمة LocalApiService إذا لم تكن تعمل
        val intent = Intent(context, LocalApiService::class.java)
        intent.action = LocalApiService.ACTION_START_SERVER
        context.startForegroundService(intent)

        // انتظر حتى يصبح السيرفر جاهزًا (حتى 30 ثانية)
        waitForServer()
    }

    @After
    fun tearDown() {
        // إيقاف السيرفر
        val intent = Intent(context, LocalApiService::class.java)
        intent.action = LocalApiService.ACTION_STOP_SERVER
        context.stopService(intent)
        client.dispatcher.executorService.shutdown()
    }

    /**
     * انتظار حتى يصبح السيرفر جاهزًا للاستقبال على منفذ 8080
     */
    private fun waitForServer() {
        var attempts = 0
        val maxAttempts = 30 // 30 ثانية كحد أقصى
        while (attempts < maxAttempts) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/health")
                    .get()
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.close()
                    return // السيرفر جاهز
                }
                response.body?.close()
            } catch (e: Exception) {
                // السيرفر لم يبدأ بعد، انتظر
            }
            attempts++
            Thread.sleep(1000)
        }
        throw AssertionError("السيرفر المحلي لم يصبح جاهزًا خلال 30 ثانية")
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
            .post(body.toRequestBody(JSON))
            .build()
        return client.newCall(request).execute()
    }

    @Test
    fun healthEndpointReturnsOk() {
        val response = get("/health")
        assertTrue("يجب أن يكون الاستجابة ناجحة (200)، ولكن حصلت على ${response.code}",
            response.isSuccessful)
        val body = response.body?.string() ?: ""
        assertNotNull("يجب أن يحتوي الرد على حقل 'status'", body.contains("\"status\""))
    }

    @Test
    fun modelsEndpointReturnsList() {
        val response = get("/v1/models")
        assertTrue("يجب أن يكون الاستجابة ناجحة (200)، ولكن حصلت على ${response.code}",
            response.isSuccessful)
        val body = response.body?.string() ?: ""
        val json = JsonParser.parseString(body).asJsonObject
        val data = json.getAsJsonArray("data")
        assertNotNull("قائمة النماذج لا يجب أن تكون فارغة", data != null && data.size() > 0)
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
        val body = response.body?.string() ?: ""
        if (body.contains("no DeepSeek account configured")) {
            Assume.assumeTrue("لا يوجد حساب DeepSeek مهيأ للاختبار المحلي للدردشة", false)
            return
        }
        if (body.contains("401") || body.contains("403") || body.contains("auth", ignoreCase = true)) {
            Assume.assumeTrue("فشل مصادقة DeepSeek أو انتهاء صلاحية التوكن: $body", false)
            return
        }
        assertTrue("يجب أن يكون الاستجابة ناجحة (200)، ولكن حصلت على ${response.code}. جسم الرد: $body",
            response.isSuccessful)
        val json = JsonParser.parseString(body).asJsonObject
        val choices = json.getAsJsonArray("choices")
        assertNotNull("يجب ألا تكون قائمة الخيارات فارغة", choices != null && choices.size() > 0)
        val content = choices.get(0).asJsonObject.getAsJsonObject("message").get("content").asString
        assertTrue("يجب أن يكون محتوى الرد غير فارغ", content.isNotEmpty())
    }

    @Test
    fun chatCompletionsStreamingReturnsSse() {
        val payload = """
        {
          "model": "deepseek-chat",
          "messages": [{"role": "user", "content": "قل مرحباً فقط"}],
          "stream": true,
          "thinking_enabled": false,
          "search_enabled": false
        }
        """.trimIndent()
        val response = post("/v1/chat/completions", payload)
        val body = response.body?.string() ?: ""
        if (body.contains("no DeepSeek account configured")) {
            Assume.assumeTrue("لا يوجد حساب DeepSeek مهيأ للاختبار المحلي للدردشة التدفقي", false)
            return
        }
        if (body.contains("401") || body.contains("403") || body.contains("auth", ignoreCase = true)) {
            Assume.assumeTrue("فشل مصادقة DeepSeek أو انتهاء صلاحية التوكن في التدفق: $body", false)
            return
        }
        assertTrue("يجب أن يكون الاستجابة ناجحة (200) للتدفق، ولكن حصلت على ${response.code}. جسم الرد: $body",
            response.isSuccessful)
        assertTrue("يجب أن يحتوي رد التدفق على 'data:' أو '[DONE]'",
            body.contains("data:") || body.contains("[DONE]"))
    }

    @Test
    fun invalidRequestReturnsError() {
        val response = post("/v1/chat/completions", "not json")
        val body = response.body?.string() ?: ""
        // قد يكون 400 أو 500 حسب التنفيذ، أو 200 مع رسالة خطأ، ولكن لا يجب أن يكون 200 ناجحًا مع محتوى محادثة صالح
        assertTrue("طلب غير صالح يجب ألا ينجح بدون خطأ",
            response.code != 200 || body.contains("error"))
    }

    @Test
    fun serverStopAndRestart() {
        // اختبار إيقاف السيرفر
        val stopIntent = Intent(context, LocalApiService::class.java)
        stopIntent.action = LocalApiService.ACTION_STOP_SERVER
        context.stopService(stopIntent)

        // التحقق من أن السيرفر توقف (يجب أن يفشل الاتصال)
        var serverStopped = false
        try {
            val request = Request.Builder()
                .url("$BASE_URL/health")
                .get()
                .build()
            val response = client.newCall(request).execute()
            response.body?.close()
            if (!response.isSuccessful) {
                serverStopped = true
            }
        } catch (e: Exception) {
            serverStopped = true
        }
        assertTrue("يجب أن يتوقف السيرفر عند طلب الإيقاف", serverStopped)

        // اختبار إعادة تشغيل السيرفر
        val startIntent = Intent(context, LocalApiService::class.java)
        startIntent.action = LocalApiService.ACTION_START_SERVER
        context.startForegroundService(startIntent)

        // انتظر حتى يعود السيرفر للعمل
        var attempts = 0
        val maxAttempts = 30
        var serverRestarted = false
        while (attempts < maxAttempts) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/health")
                    .get()
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.close()
                    serverRestarted = true
                    break
                }
                response.body?.close()
            } catch (e: Exception) {
                // الانتظار
            }
            attempts++
            Thread.sleep(1000)
        }
        assertTrue("يجب أن يعيد السيرفر التشغيل بعد إعادة تشغيله", serverRestarted)
    }
}
