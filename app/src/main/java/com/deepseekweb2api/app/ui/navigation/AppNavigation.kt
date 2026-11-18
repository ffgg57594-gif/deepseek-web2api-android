package com.deepseekweb2api.app.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.deepseekweb2api.app.client.DeepSeekClient
import com.deepseekweb2api.app.data.Account
import com.deepseekweb2api.app.data.AccountRepository
import com.deepseekweb2api.app.server.KtorServer
import com.deepseekweb2api.app.ui.screens.AddEditAccountScreen
import com.deepseekweb2api.app.ui.screens.ApiConfigScreen
import com.deepseekweb2api.app.ui.screens.AccountManagerScreen
import com.deepseekweb2api.app.ui.screens.DashboardScreen
import com.deepseekweb2api.app.ui.screens.LogsScreen
import com.deepseekweb2api.app.ui.screens.ModelSelectionScreen
import com.deepseekweb2api.app.ui.screens.SettingsScreen
import com.deepseekweb2api.app.ui.screens.ServerControlScreen
import com.deepseekweb2api.app.ui.screens.StatisticsScreen
import com.deepseekweb2api.app.ui.theme.DeepSeekTheme

sealed class Screen {
    object Dashboard : Screen()
    object Accounts : Screen()
    object AddAccount : Screen()
    object Models : Screen()
    object ApiConfig : Screen()
    object ServerControl : Screen()
    object Statistics : Screen()
    object Logs : Screen()
    object Settings : Screen()

    val title: String = when (this) {
        is Dashboard -> "لوحة المعلومات"
        is Accounts -> "الحسابات"
        is AddAccount -> "إضافة حساب"
        is Models -> "النماذج"
        is ApiConfig -> "إعدادات API"
        is ServerControl -> "التحكم بالخادم"
        is Statistics -> "الإحصائيات"
        is Logs -> "السجلات"
        is Settings -> "الإعدادات"
    }
}

@Composable
fun AppNavigation() {
    var navStack by remember { mutableStateOf(listOf<Screen>(Screen.Dashboard)) }
    val screen: Screen by remember { navStack.last() }

    val repo by remember { mutableStateOf(AccountRepository()) }
    val accounts by repo.value.accounts.collectAsState(initialValue = emptyList<Account>())

    var server by remember { mutableStateOf<KtorServer?>(null) }
    var isRunning by remember { mutableStateOf(false) }
    var deepSeekClient by remember { mutableStateOf<DeepSeekClient?>(null) }

    fun push(s: Screen) { navStack = navStack + s }
    fun pop() { if (navStack.size > 1) navStack = navStack.dropLast(1) }

    DeepSeekTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(text = screen.title) },
                    navigationIcon = if (navStack.size > 1) {
                        IconButton(onClick = { pop() }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                        }
                    } else null
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (screen) {
                    Screen.Dashboard -> DashboardScreen(
                        onAccounts = { push(Screen.Accounts) },
                        onServer = { push(Screen.ServerControl) },
                        onModels = { push(Screen.Models) },
                        onLogs = { push(Screen.Logs) },
                        onSettings = { push(Screen.Settings) },
                        stats = repo.value.stats
                    )
                    Screen.Accounts -> AccountManagerScreen(
                        onBack = { pop() },
                        onAdd = { push(Screen.AddAccount) },
                        accounts = accounts,
                        onTest = { acct ->
                            repo.value.setState(acct.id, "busy")
                            try {
                                val client = deepSeekClient.value ?: DeepSeekClient.create(
                                    acct.token,
                                    acct.cookies
                                )
                                deepSeekClient.value = client
                                client.createSession()
                                repo.value.setState(acct.id, "idle")
                                Toast.makeText(
                                    androidx.compose.ui.platform.LocalContext.current,
                                    "الحساب سليم",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                repo.value.setState(acct.id, "error", e.message)
                            }
                        },
                        onDelete = { acct -> repo.value.remove(acct.id) }
                    )
                    Screen.AddAccount -> AddEditAccountScreen(
                        onBack = { pop() },
                        repo = repo.value,
                        onSaved = { pop() }
                    )
                    Screen.Models -> ModelSelectionScreen(
                        onBack = { pop() },
                        onSelect = {}
                    )
                    Screen.ApiConfig -> ApiConfigScreen(onBack = { pop() })
                    Screen.ServerControl -> ServerControlScreen(
                        onBack = { pop() },
                        server = server,
                        isRunning = isRunning,
                        onToggle = {
                            if (!isRunning) {
                                server = KtorServer(8080, deepSeekClient.value)
                                server?.start()
                                isRunning = true
                            } else {
                                server?.stop()
                                server = null
                                isRunning = false
                            }
                        }
                    )
                    Screen.Statistics -> StatisticsScreen(
                        onBack = { pop() },
                        stats = repo.value.stats
                    )
                    Screen.Logs -> LogsScreen(onBack = { pop() })
                    Screen.Settings -> SettingsScreen(onBack = { pop() })
                }
            }
        }
    }
}
