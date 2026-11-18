package com.deepseekweb2api.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ApiConfigScreen(
    onBack: () -> Unit,
    baseUrl: String = "http://127.0.0.1:8080/v1",
    apiKey: String = "deepseek-web2api-local"
) {
    val ctx = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "إعدادات API", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "الرابط الأساسي (Base URL)", style = MaterialTheme.typography.bodyMedium)
        Text(text = baseUrl, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "مفتاح API", style = MaterialTheme.typography.bodyMedium)
        Text(text = apiKey, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                copyClip(ctx, baseUrl)
                Toast.makeText(ctx, "تم نسخ الرابط", Toast.LENGTH_SHORT).show()
            }) { Text(text = "نسخ الرابط") }
            Button(onClick = {
                copyClip(ctx, apiKey)
                Toast.makeText(ctx, "تم نسخ المفتاح", Toast.LENGTH_SHORT).show()
            }) { Text(text = "نسخ المفتاح") }
        }

        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onBack) { Text(text = "الرجوع") }
    }
}

private fun copyClip(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("deepseek-api", text))
}
