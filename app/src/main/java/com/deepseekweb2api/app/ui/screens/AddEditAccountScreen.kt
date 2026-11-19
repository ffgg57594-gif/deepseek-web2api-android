package com.deepseekweb2api.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.deepseekweb2api.app.client.DeepSeekClient
import com.deepseekweb2api.app.data.Account
import com.deepseekweb2api.app.data.AccountRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AddEditAccountScreen(
    onBack: () -> Unit,
    repo: AccountRepository,
    editAccount: Account? = null,
    onSaved: () -> Unit,
    userAgent: String = DeepSeekClient.DEFAULT_USER_AGENT,
    secChUa: String = DeepSeekClient.DEFAULT_SEC_CH_UA,
) {
    val ctx = LocalContext.current
    var email by remember { mutableStateOf(editAccount?.email ?: "") }
    var token by remember { mutableStateOf(editAccount?.token ?: "") }
    var cookies by remember { mutableStateOf(editAccount?.cookies ?: "") }

    var testResult by remember { mutableStateOf<DeepSeekClient.ConnectionTestResult?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var validationWarning by remember { mutableStateOf<String?>(null) }

    // ------------------------------------------------------------------
    // Input cleaning helpers
    // ------------------------------------------------------------------
    fun cleanRawInput(text: String): String {
        var t = text.trim()
        t = t.replace("\r", "").replace("\n", "")
        // Remove "Bearer " prefix if present (case-insensitive)
        if (t.startsWith("Bearer ", ignoreCase = true)) {
            t = t.removePrefix("Bearer ").removePrefix("bearer ").trim()
        }
        return t
    }

    fun extractJsonValue(text: String): String {
        try {
            val obj = com.google.gson.JsonParser.parseString(text).asJsonObject
            val value = obj.get("value")
            if (value != null && !value.isJsonNull && value.isJsonPrimitive) {
                return value.asString
            }
        } catch (_: Exception) {
            // Not JSON, return as-is
        }
        return text
    }

    fun checkNonAscii(text: String, fieldLabel: String): String? {
        for (i in text.indices) {
            if (text[i] > 0x7F) {
                return "${ctx.getString(R.string.warning_non_ascii, i + 1, text[i])}"
            }
        }
        return null
    }

    fun save() {
        val cleanedToken = cleanRawInput(token)
        val cleanedCookies = cleanRawInput(cookies)

        val builder = Account.Builder(editAccount)
        builder.email(email.ifBlank { null })
        builder.token(cleanedToken)
        builder.cookies(cleanedCookies.ifBlank { null })
        val built = builder.build()

        if (editAccount == null) {
            repo.add(built)
        } else {
            repo.update(built.id) {
                email(email.ifBlank { null })
                token(cleanedToken)
                cookies(cleanedCookies.ifBlank { null })
            }
        }
        Toast.makeText(ctx, "تم حفظ الحساب", Toast.LENGTH_SHORT).show()
        onSaved()
    }

    fun runTest() {
        val cleanedToken = cleanRawInput(token)
        val cleanedCookies = cleanRawInput(cookies)

        // JSON value extraction
        var finalToken = extractJsonValue(cleanedToken)
        var finalCookies = extractJsonValue(cleanedCookies)

        // Non-ASCII check
        val tokenWarning = checkNonAscii(finalToken, ctx.getString(R.string.field_token))
        val cookiesWarning = checkNonAscii(finalCookies, ctx.getString(R.string.field_cookies))
        validationWarning = tokenWarning ?: cookiesWarning
        if (validationWarning != null) {
            Toast.makeText(ctx, validationWarning, Toast.LENGTH_LONG).show()
            return
        }

        isTesting = true
        testResult = null

        CoroutineScope(Dispatchers.IO).launch {
            val result = DeepSeekClient.create(finalToken, finalCookies, userAgent, secChUa).testConnection()
            withContext(Dispatchers.Main) {
                testResult = result
                isTesting = false
                if (result.success) {
                    Toast.makeText(ctx, "تم الاتصال بنجاح", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(ctx, "فشل الاتصال", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = if (editAccount == null) "إضافة حساب" else "تعديل حساب",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("البريد الإلكتروني (اختياري)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("التفويض (Authorization token)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = cookies,
            onValueChange = { cookies = it },
            label = { Text("كوكيز (Cookie header)") },
            modifier = Modifier.fillMaxWidth()
        )

        // Validation warning
        validationWarning?.let { warning ->
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.Text(
                text = warning,
                style = MaterialTheme.typography.bodySmall,
                color = androidx.compose.ui.graphics.Color(0xFFB3261E)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Test connection button
        Button(
            onClick = { runTest() },
            enabled = !isTesting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isTesting) {
                Text(text = ctx.getString(R.string.testing_connection))
            } else {
                Text(text = ctx.getString(R.string.test_connection))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Test result display
        testResult?.let { result ->
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = if (result.success) androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer else androidx.compose.material3.MaterialTheme.colorScheme.errorContainer
                )
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (result.success) ctx.getString(R.string.connection_success) else ctx.getString(R.string.connection_failed),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (result.success) androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer else androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    result.httpCode?.let { code ->
                        Text(text = ctx.getString(R.string.http_code, code), style = MaterialTheme.typography.bodyMedium)
                        result.httpCodeMeaning?.let { meaning ->
                            Text(text = ctx.getString(R.string.http_code_meaning, meaning), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    result.exceptionName?.let { name ->
                        Text(text = ctx.getString(R.string.exception_name, name), style = MaterialTheme.typography.bodyMedium)
                        result.exceptionMessage?.let { msg ->
                            Text(text = ctx.getString(R.string.exception_message, msg), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    if (result.isHtmlResponse) {
                        Text(text = ctx.getString(R.string.html_response_warning), style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color(0xFFB07D00))
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    result.bodyPreview?.let { preview ->
                        Text(text = ctx.getString(R.string.response_body), style = MaterialTheme.typography.bodySmall)
                        Text(text = preview, style = MaterialTheme.typography.bodySmall, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    result.bodyFull?.let { full ->
                        Text(text = ctx.getString(R.string.response_body_full), style = MaterialTheme.typography.bodySmall)
                        Text(text = full, style = MaterialTheme.typography.bodySmall, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { save() }) {
            Text(text = "حفظ")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text(text = "إلغاء") }
    }
}
