package com.deepseekweb2api.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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
            Text(text = "لا توجد حسابات مضافّة.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        } else {
            LazyColumn {
                items(accounts, key = { it.id }) { acct ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Column {
                                Text(text = acct.email ?: acct.id, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = when (acct.state) {
                                        "idle" -> "غير نشط"
                                        "busy" -> "مشغول"
                                        "error" -> "خطأ"
                                        else -> acct.state
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when (acct.state) {
                                        "idle" -> MaterialTheme.colorScheme.success
                                        "error" -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (acct.lastError != null) {
                                    Text(text = "آخر خطأ: ${acct.lastError}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                                Button(
                                    onClick = { onTest(acct) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text(text = "اختبار")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { onDelete(acct) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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
