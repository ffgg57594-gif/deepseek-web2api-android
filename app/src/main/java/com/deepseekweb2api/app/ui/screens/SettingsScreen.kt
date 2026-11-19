package com.deepseekweb2api.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deepseekweb2api.app.client.DeepSeekClient

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    deepThinkEnabled: Boolean = false,
    webSearchEnabled: Boolean = false,
    functionCallingEnabled: Boolean = false,
    onToggle: (String, Boolean) -> Unit = { _, _ -> },
    userAgent: String = DeepSeekClient.DEFAULT_USER_AGENT,
    secChUa: String = DeepSeekClient.DEFAULT_SEC_CH_UA,
    onUserAgentChange: (String) -> Unit = { },
    onSecChUaChange: (String) -> Unit = { },
) {
    var dt by remember { mutableStateOf(deepThinkEnabled) }
    var ws by remember { mutableStateOf(webSearchEnabled) }
    var fc by remember { mutableStateOf(functionCallingEnabled) }
    var ua by remember { mutableStateOf(userAgent) }
    var scua by remember { mutableStateOf(secChUa) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "الإعدادات", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(20.dp))
        SettingRow("التفكير العميق (DeepThink)", "محتويات الاستدلال", dt) {
            dt = it; onToggle("deep_think", it)
        }
        SettingRow("البحث على الويب", "تضمين نتائج البحث", ws) {
            ws = it; onToggle("web_search", it)
        }
        SettingRow("استدعاء الدوال (DSML)", "السماح باستدعاء الدوال", fc) {
            fc = it; onToggle("function_calling", it)
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "وكيل المستخدم (User-Agent)", style = MaterialTheme.typography.bodyLarge)
        Text(text = "قيمة تُرسل مع كل طلب للخادم. يمكن لصق وكيل المتصفح الذي أخذت منه الكوكيز.", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = ua,
            onValueChange = { ua = it; onUserAgentChange(it) },
            label = { Text("User-Agent") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Sec-Ch-Ua", style = MaterialTheme.typography.bodyLarge)
        Text(text = "مstead من valeur المتصفح. يُنصح بنسخها بالضبط من تاب Network في المتصفح.", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = scua,
            onValueChange = { scua = it; onSecChUaChange(it) },
            label = { Text("Sec-Ch-Ua") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "نصيحة: انسخ الـ Cookie header كاملاً من تاب Network (ليس document.cookie)، واستخدم نفس المتصفح والشبكة.", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onBack) { Text(text = "الرجوع") }
    }
}

@Composable
private fun SettingRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = summary, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
