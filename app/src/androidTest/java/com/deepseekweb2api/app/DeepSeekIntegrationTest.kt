package com.deepseekweb2api.app

import android.app.Instrumentation
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.deepseekweb2api.app.client.DeepSeekClient
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith

/**
 * اختبار التكامل الحقيقي مع خدمة DeepSeek Web.
 * يُنفذ فقط إذا كانتسرا DEEPSEEK_AUTH_TOKEN و DEEPSEEK_COOKIE مضبوطة
 * عبر GitHub Secrets وتمريرها كInstrumentation Arguments.
 *
 * لا يُخزن أي أسرار في الكود ولا يطبعها في السجلات.
 */
@RunWith(AndroidJUnit4::class)
class DeepSeekIntegrationTest {

    @Test
    fun realDeepSeekWebIntegration() {
        val args = InstrumentationRegistry.getArguments()
        val token: String? = args.getString("deepseekToken")
        val cookiesB64: String? = args.getString("deepseekCookiesB64")
        val cookies: String? = cookiesB64?.let { String(Base64.decode(it, Base64.NO_WRAP)) }

        // إذا لم توجد سرر، נסجل NOT TESTED ونترك الاختبار يتخطى نفسه
        Assume.assumeTrue(
            "deepseekToken و deepseekCookiesB64 غير مضبوطة — اختبار التكامل الحقيقي غير منفذ (NOT TESTED)",
            token.isNotEmpty() && cookies.isNotEmpty()
        )

        // هذا الجزء يُنفذ فقط إذا كانت البيانات reales موجودة
        val client = DeepSeekClient.create(token.trim(), cookies.trim())
        val sessionId = runBlocking { client.createSession() }
        assert(sessionId.isNotEmpty()) { "يجب إنشاء جلسة ناجحة" }

        val result = runBlocking {
            client.chat(
                sessionId = sessionId,
                prompt = "مرحبا، أجب بكلمة واحدة فقط: اختبار",
                modelType = "deepseek-chat",
                thinkingEnabled = false,
                searchEnabled = false,
                parentMessageId = null
            )
        }

        assert(result.content.isNotEmpty()) { "يجب أن تحتوي الإجابة على محتوى فعلي من DeepSeek Web" }
        // لا نطبع المحتوى في السجلات لحماية الخصوصية
    }
}
