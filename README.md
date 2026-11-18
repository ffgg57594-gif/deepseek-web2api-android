# DeepSeek Web2API — Native Kotlin Android

تطبيق أندرويد أصلي أعيد كتابته بالكامل من مشروع [snake-aabb-wtf/deepseek-web2api-free](https://github.com/snake-aabb-wtf/deepseek-web2api-free)، باستخدام Kotlin فقط.

## التقنيات المستخدمة
- Kotlin فقط للتطبيق والخادم الخلفي.
- Jetpack Compose لكل واجهة المستخدم ( RTL باللغة العربية).
- OkHttp لشبكة HTTP.
- Kotlin Coroutines و Flow للعمليات غير المتزامنة والبث.
- Ktor Server كخادم HTTP مدمج يستمع على `127.0.0.1:8080`.
- Android Keystore (عبر `EncryptedSharedPreferences`) لتشفير بيانات الاعتماد عند التخزين.
- دعم ARM64، توافق Android 15 و 16.

## ما لا يستخدمه المشروع
لا Python، لا Node.js، لا Termux، لا WebView، لا خادم خارجي (VPS)، لا API رسمي من DeepSeek.

## نقاط النهاية
- `GET /health`
- `GET /v1/models`
- `POST /v1/chat/completions` (متوافق مع OpenAI، بث SSE وغير بث)

## الشاشات
1. لوحة المعلومات
2. إدارة الحسابات (إضافة، تعديل، حذف، اختبار)
3. إضافة/تعديل حساب
4. اختيار النموذج
5. إعدادات API
6. التحكم بالخادم (بدء/إيقاف، حالة، نسخ الرابط والمفتاح)
7. الإحصائيات
8. السجلات (البيانات الحساسة مخفية)
9. الإعدادات (DeepThink، بحث ويب، استدعاء دوال)

## البناء
يتم البناء حصرياً عبر GitHub Actions ويصدر APK إلى GitHub Releases.

## تعليمات AndroidHarness
انظر ملف [ANDROID_HARNESS.md](ANDROID_HARNESS.md).

## الرابط
https://github.com/ffgg57594-gif/deepseek-web2api-android
