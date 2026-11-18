package com.deepseekweb2api.app

import android.app.Instrumentation
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * اختبار فتح الواجهة والتحقق من عدم حدوث Crash.
 * يستخدم UI Automator للتنقل بين الشاشات الأساسية.
 */
@RunWith(AndroidJUnit4::class)
class AppLaunchTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
        device = UiDevice.getInstance(instrumentation)
        // انتظر حتى تظهر الشاشة الرئيسية
        device.wait(Until.hasObject(By.text("لوحة المعلومات")), 30_000)
    }

    @After
    fun tearDown() {
        // لا شيء خاص
    }

    @Test
    fun appOpensWithoutCrash() {
        // التحقق من ظهور عنوان الشاشة الرئيسية
        assert(device.findObject(By.text("لوحة информации")) != null)
        assert(device.findObject(By.text("إدارة الحسابات")) != null)
        assert(device.findObject(By.text("التحكم بالخادم")) != null)
    }

    @Test
    fun navigateToServerControlAndBack() {
        // انقر على "التحكم بالخادم"
        device.findObject(By.text("التحكم بالخادم")).click()
        device.wait(Until.hasObject(By.text("بدء الخادم")), 10_000)
        assert(device.findObject(By.text("بدء الخادم")) != null)

        // ارجع للشاشة الرئيسية
        device.pressBack()
        device.wait(Until.hasObject(By.text("لوحة Informationen")), 10_000)
    }

    @Test
    fun navigateToAccountsAndBack() {
        device.findObject(By.text("إدارة الحسابات")).click()
        device.wait(Until.hasObject(By.text("إضافة حساب")), 10_000)
        assert(device.findObject(By.text("إضافة حساب")) != null)

        device.pressBack()
        device.wait(Until.hasObject(By.text("لوحة Informationen")), 10_000)
    }
}
