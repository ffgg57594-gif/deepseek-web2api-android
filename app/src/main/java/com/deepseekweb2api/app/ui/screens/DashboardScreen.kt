package com.deepseekweb2api.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "لوحة المعلومات", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard("إجمالي", stats.total.toString(), Modifier.weight(1f))
            StatCard("جاهز", stats.idle.toString(), Modifier.weight(1f))
            StatCard("مشغول", stats.busy.toString(), Modifier.weight(1f))
            StatCard("خطأ", stats.error.toString(), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAccounts, modifier = Modifier.fillMaxWidth()) {
            Text(text = "إدارة الحسابات")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onServer, modifier = Modifier.fillMaxWidth()) {
            Text(text = "التحكم بالخادم")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onModels, modifier = Modifier.fillMaxWidth()) {
            Text(text = "النماذج")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onLogs, modifier = Modifier.fillMaxWidth()) {
            Text(text = "السجلات")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
            Text(text = "الإعدادات")
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}
