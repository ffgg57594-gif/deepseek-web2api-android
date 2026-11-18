package com.deepseekweb2api.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    deepThinkEnabled: Boolean = false,
    webSearchEnabled: Boolean = false,
    functionCallingEnabled: Boolean = false,
    onToggle: (String, Boolean) -> Unit = { _, _ -> }
) {
    var dt by remember { mutableStateOf(deepThinkEnabled) }
    var ws by remember { mutableStateOf(webSearchEnabled) }
    var fc by remember { mutableStateOf(functionCallingEnabled) }

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
