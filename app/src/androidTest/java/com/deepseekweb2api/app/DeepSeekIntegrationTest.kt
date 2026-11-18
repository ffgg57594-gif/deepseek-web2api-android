package com.deepseekweb2api.app

import android.app.Instrumentation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.deepseekweb2api.app.client.DeepSeekClient
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith

/**
 * اختبار التكامل الحقيقي مع خدمة DeepSeek Web.
 * يُنفذ فقط إذا كانتcrets DEEPSEEK_AUTH_TOKEN و DEEPSEEK_COOKIE مضبوطة
 * عبر GitHub Secrets أو متغيرات البيئة.
 *
 * لا يُخزن أي أسرار في الكود ولا يطبعها في السجلات.
 */
@RunWith(AndroidJUnit4::class)
class DeepSeekIntegrationTest {

    companion object {
        const val AUTH_TOKEN_ENV = "DEEPSEEK_AUTH_TOKEN"
        const val COOKIES_ENV = "DEEPSEEK_COOKIE"
    }

    private fun getEnv(name: String): String? {
        val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
        return instrumentation.context.getString(
            instrumentation.context.resources.getIdentifier(name, "string", instrumentation.context.packageName)
        ).takeIf { it.isNotEmpty() }
            ?: System.getenv(name)
    }

    @Test
    fun realDeepSeekWebIntegration() {
        val token = getEnv(AUTH_TOKEN_ENV)
        val cookies = getEnv(COOKIES_ENV)

        Assume.assumeTrue(
            "DEEPSEEK_AUTH_TOKEN و DEEPSEEK_COOKIE غير مضبوطة — اختبار التكامل الحقيقي غير مُنفذ (NOT TESTED)",
            token != null && token.isNotEmpty() && cookies != null && cookies.isNotEmpty()
        )

        // هذا الجزء يُنفذ فقط إذا كانت البيانات reales موجودة
        val client = DeepSeekClient.create(token, cookies)
        val sessionId = client.createSession()
        assert(sessionId.isNotEmpty()) { "يجب إنشاء جلسة ناجحة" }

        val result = client.chat(
            sessionId = sessionId,
            prompt = "مرحبا، أجب بكلمة واحدة فقط: اختبار",
            modelType = "deepseek-chat",
            thinkingEnabled = false,
            searchEnabled = false,
            parentMessageId = null
        )

        assert(result.content.isNotEmpty()) { "يجب أن تحتوي الإجابة على محتوى فعلي من DeepSeek Web" }
        // لا نطبع المحتوى في السجلات لحماية الخصوصية
    }
}
