package com.deepseekweb2api.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.deepseekweb2api.app.data.Account
import com.deepseekweb2api.app.data.AccountRepository

@Composable
fun AddEditAccountScreen(
    onBack: () -> Unit,
    repo: AccountRepository,
    editAccount: Account? = null,
    onSaved: () -> Unit
) {
    val ctx = LocalContext.current
    var email by remember { mutableStateOf(editAccount?.email ?: "") }
    var token by remember { mutableStateOf(editAccount?.token ?: "") }
    var cookies by remember { mutableStateOf(editAccount?.cookies ?: "") }

    fun save() {
        val builder = Account.Builder(editAccount)
        builder.email(email.ifBlank { null })
        builder.token(token.trim())
        builder.cookies(cookies.ifBlank { null })
        val built = builder.build()

        if (editAccount == null) {
            repo.add(built)
        } else {
            repo.update(built.id) {
                email(email.ifBlank { null })
                token(token.trim())
                cookies(cookies.ifBlank { null })
            }
        }
        Toast.makeText(ctx, "تم حفظ الحساب", Toast.LENGTH_SHORT).show()
        onSaved()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = if (editAccount == null) "إضافة حساب" else "تعديل حساب",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("البريد الإلكتروني (اختياري)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("التفويض (Authorization token)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = cookies,
            onValueChange = { cookies = it },
            label = { Text("كوكيز (Cookie header)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { save() }) {
            Text(text = "حفظ")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text(text = "إلغاء") }
    }
}
