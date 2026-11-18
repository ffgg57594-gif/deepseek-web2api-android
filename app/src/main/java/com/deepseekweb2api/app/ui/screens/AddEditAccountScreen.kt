package com.deepseekweb2api.app.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Keyboard
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
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
        val acct = editAccount?.let {
            Account.Builder(it)
                .email(email)
                .token(token)
                .cookies(if (cookies.isBlank()) null else cookies)
                .build()
        } ?: Account.Builder()
            .email(email)
            .token(token)
            .cookies(if (cookies.isBlank()) null else cookies)
            .build()
        repo.add(acct)
        onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = if (editAccount == null) "إضافة حساب" else "تعديل حساب") })
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(text = "البريد الإلكتروني (اختياري)") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text(text = "التفويض (Authorization token)") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            OutlinedTextField(
                value = cookies,
                onValueChange = { cookies = it },
                label = { Text(text = "كوكيز (Cookie header)") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { save(); Toast.makeText(ctx, "تم حفظ الحساب", Toast.LENGTH_SHORT).show() }) {
                Text(text = "حفظ")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onBack) { Text(text = "إلغاء") }
        }
    }
}
