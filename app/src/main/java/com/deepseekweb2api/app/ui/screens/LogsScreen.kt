package com.deepseekweb2api.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LogsScreen(
    onBack: () -> Unit,
    logs: List<LogEntry> = emptyList(),
    onClear: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "السجلات (البيانات الحساسة مخفية)", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onClear) { Text(text = "مسح السجلات") }
        Spacer(modifier = Modifier.height(12.dp))
        if (logs.isEmpty()) {
            Text(text = "لا توجد سجلات.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        } else {
            LazyColumn {
                items(logs, key = { it.id }) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        androidx.compose.foundation.layout.Row(modifier = Modifier.padding(12.dp)) {
                            Column {
                                Text(text = entry.message, style = MaterialTheme.typography.bodyMedium)
                                Text(text = entry.time, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onBack) { Text(text = "الرجوع") }
    }
}

data class LogEntry(val id: String, val message: String, val time: String)
