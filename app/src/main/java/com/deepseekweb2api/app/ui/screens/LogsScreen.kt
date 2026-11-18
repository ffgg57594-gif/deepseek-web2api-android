package com.deepseekweb2api.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class LogEntry(val id: String, val message: String, val time: String)

@Composable
fun LogsScreen(
    onBack: () -> Unit,
    logs: List<LogEntry> = emptyList(),
    onClear: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "السجلات (البيانات الحساسة مخفية)",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onClear) { Text(text = "مسح السجلات") }
        Spacer(modifier = Modifier.height(12.dp))

        if (logs.isEmpty()) {
            Text(text = "لا توجد سجلات.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn {
                items(logs) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = entry.message, style = MaterialTheme.typography.bodyMedium)
                            Text(text = entry.time, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBack) { Text(text = "الرجوع") }
    }
}
