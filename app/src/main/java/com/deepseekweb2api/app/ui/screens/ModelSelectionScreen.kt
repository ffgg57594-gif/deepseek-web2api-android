package com.deepseekweb2api.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
fun ModelSelectionScreen(
    onBack: () -> Unit,
    models: List<String> = listOf("deepseek-chat", "deepseek-reasoner"),
    selected: String = "deepseek-chat",
    onSelect: (String) -> Unit
) {
    var sel by remember { mutableStateOf(selected) }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "اختر النموذج", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        models.forEach { model ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = sel == model, onClick = { sel = model; onSelect(model) })
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = sel == model,
                    onClick = { sel = model; onSelect(model) }
                )
                Text(text = model, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onBack) { Text(text = "الرجوع") }
    }
}
