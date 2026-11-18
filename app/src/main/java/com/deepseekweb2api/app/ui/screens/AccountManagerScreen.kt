package com.deepseekweb2api.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.deepseekweb2api.app.data.Account

@Composable
fun AccountManagerScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    accounts: List<Account>,
    onTest: (Account) -> Unit,
    onDelete: (Account) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "إدارة الحسابات", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAdd) {
            Text(text = "إضافة حساب")
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (accounts.isEmpty()) {
            Text(text = "لا توجد حسابات مضافة.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn {
                items(accounts) { acct ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = acct.email ?: acct.id,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = when (acct.state) {
                                        "idle" -> "جاهز"
                                        "busy" -> "مشغول"
                                        "error" -> "خطأ"
                                        else -> acct.state
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when (acct.state) {
                                        "error" -> Color(0xFFB3261E)
                                        "busy" -> Color(0xFFB07D00)
                                        else -> Color(0xFF1B7F3B)
                                    }
                                )
                                acct.lastError?.let {
                                    Text(text = "آخر خطأ: $it", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(onClick = { onTest(acct) }) {
                                    Text(text = "اختبار")
                                }
                                Button(
                                    onClick = { onDelete(acct) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text(text = "حذف")
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBack) { Text(text = "الرجوع") }
    }
}
