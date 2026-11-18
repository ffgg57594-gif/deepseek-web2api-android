package com.deepseekweb2api.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.deepseekweb2api.app.server.KtorServer

@Composable
fun ServerControlScreen(
    onBack: () -> Unit,
    server: KtorServer?,
    isRunning: Boolean,
    onToggle: () -> Unit
) {
    val ctx = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "التحكم بالخادم", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onToggle,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(text = if (isRunning) "إيقاف الخادم" else "بدء الخادم")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "الحالة: ${if (isRunning) "يعمل على 127.0.0.1:8080" else "متوقف"}",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Button(onClick = { copyToClipboard(ctx, "http://127.0.0.1:8080") }) {
                Text(text = "نسخ رابط API")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { copyToClipboard(ctx, "deepseek-web2api-local") }) {
                Text(text = "نسخ مفتاح API")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onBack) { Text(text = "الرجوع") }
    }
}

private fun copyToClipboard(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(android.content.ClipData.newPlainText("deepseek-api", text))
    Toast.makeText(ctx, "تم النسخ: $text", Toast.LENGTH_SHORT).show()
}
