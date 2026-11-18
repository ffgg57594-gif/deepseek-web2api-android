# Keep Gson model classes used for JSON (de)serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.deepseekweb2api.app.data.** { *; }
-keep class com.deepseekweb2api.app.client.** { *; }
