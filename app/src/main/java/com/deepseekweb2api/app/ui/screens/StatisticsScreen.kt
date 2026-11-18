package com.deepseekweb2api.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
fun StatisticsScreen(
    onBack: () -> Unit,
    stats: AccountStats = AccountStats(0, 0, 0, 0)
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "إحصائيات الطلبات", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(24.dp))
        StatRow("إجمالي الحسابات", stats.total.toString())
        StatRow("نشيط", stats.idle.toString())
        StatRow("مشغول", stats.busy.toString())
        StatRow("خطأ", stats.error.toString())
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onBack) { Text(text = "الرجوع") }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.End)
    }
}
