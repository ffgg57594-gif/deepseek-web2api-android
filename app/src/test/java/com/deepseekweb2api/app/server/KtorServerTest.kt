package com.deepseekweb2api.app.server

import com.deepseekweb2api.app.client.DeepSeekClient
import io.ktor.server.engine.ApplicationEngine
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * اختبار JVM أساسي لـ KtorServer.
 * يتحقق من أن الخادم يمكن إنشاؤه وبدءه وإيقافه دون Crash.
 * لا يحتاج Android Emulator — يعمل على أي JVM.
 */
class KtorServerTest {

    @Test
    fun ktorServerShouldStartAndStopWithoutCrash() {
        val server = KtorServer(port = 0, deepSeekClient = null) // المنفذ 0 = أي منفذ متاح
        assertNotNull("يجب إنشاء كائن الخادم", server)

        server.start()
        assertNotNull("يجب أن يكون الخادم يعمل بعد start()", server.server)

        server.stop()
        assertNull("يجب أن يكون الخادم فارغًا بعد stop()", server.server)
    }

    @Test
    fun ktorServerWithNullClientShouldBeCreated() {
        // العميل معدوم (لا يوجد حساب مضبوط) — يجب ألا ينهار الخادم عند الإنشاء
        val server = KtorServer(port = 0, deepSeekClient = null)
        assertNotNull(server)
        server.start()
        server.stop()
    }

    @Test
    fun deepSeekClientCreateShouldNotCrash() {
        // إنشاء عميل DeepSeek ببيانات اختبار وهمية — لا يرسل طلبات حقيقية
        val client = DeepSeekClient.create(token = "test-token", cookies = null)
        assertNotNull(client)
    }
}
