package com.deepseekweb2api.app

import android.app.Instrumentation
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
        assertTrue("الشاشة الرئيسية يجب أن تظهر خلال 30 ثانية",
            device.wait(Until.hasObject(By.text("لوحة المعلومات")), 30_000))
    }

    @After
    fun tearDown() {
        // لا شيء خاص
    }

    @Test
    fun appOpensWithoutCrash() {
        // التحقق من ظهور عنوان الشاشة الرئيسية باستخدام JUnit assertions
        assertNotNull("عنوان 'لوحة المعلومات' يجب أن يظهر",
            device.findObject(By.text("لوحة المعلومات")))
        assertNotNull("عنوان 'إدارة الحسابات' يجب أن يظهر",
            device.findObject(By.text("إدارة الحسابات")))
        assertNotNull("عنوان 'التحكم بالخادم' يجب أن يظهر",
            device.findObject(By.text("التحكم بالخادم")))
    }

    @Test
    fun navigateToServerControlAndBack() {
        // انقر على "التحكم بالخادم"
        device.findObject(By.text("التحكم بالخادم")).click()
        assertTrue("شاشة 'التحكم بالخادم' يجب أن تظهر خلال 10 ثوانٍ",
            device.wait(Until.hasObject(By.text("بدء الخادم")), 10_000))
        assertNotNull("زر 'بدء الخادم' يجب أن يظهر",
            device.findObject(By.text("بدء الخادم")))

        // ارجع للشاشة الرئيسية
        device.pressBack()
        assertTrue("الشاشة الرئيسية يجب أن تظهر خلال 10 ثوانٍ",
            device.wait(Until.hasObject(By.text("لوحة المعلومات")), 10_000))
    }

    @Test
    fun navigateToAccountsAndBack() {
        device.findObject(By.text("إدارة الحسابات")).click()
        assertTrue("شاشة 'إدارة الحسابات' يجب أن تظهر خلال 10 ثوانٍ",
            device.wait(Until.hasObject(By.text("إضافة حساب")), 10_000))
        assertNotNull("زر 'إضافة حساب' يجب أن يظهر",
            device.findObject(By.text("إضافة حساب")))

        device.pressBack()
        assertTrue("الشاشة الرئيسية يجب أن تظهر خلال 10 ثوانٍ",
            device.wait(Until.hasObject(By.text("لوحة المعلومات")), 10_000))
    }
}
