package com.deepseekweb2api.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.deepseekweb2api.app.data.AccountStats

@Composable
fun DashboardScreen(
    onAccounts: () -> Unit,
    onServer: () -> Unit,
    onModels: () -> Unit,
    onLogs: () -> Unit,
    onSettings: () -> Unit,
    stats: AccountStats
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "لوحة المعلومات", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCard("إجمالي", stats.total.toString())
            StatCard("نشيط", stats.idle.toString())
            StatCard("مشغول", stats.busy.toString())
            StatCard("خطأ", stats.error.toString())
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAccounts) { Text(text = "إدارة الحسابات") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onServer) { Text(text = "التحكم بالخادم") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onModels) { Text(text = "النماذج") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onLogs) { Text(text = "السجلات") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onSettings) { Text(text = "الإعدادات") }
    }
}

@Composable
private fun StatCard(title: String, value: String) {
    Card(
        modifier = Modifier.weight(1f).padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}
