package com.deepseekweb2api.app.ui.navigation

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
import com.deepseekweb2api.app.data.Account
import com.deepseekweb2api.app.data.AccountRepository
import com.deepseekweb2api.app.server.KtorServer
import com.deepseekweb2api.app.ui.screens.AccountManagerScreen
import com.deepseekweb2api.app.ui.screens.DashboardScreen
import com.deepseekweb2api.app.ui.screens.ServerControlScreen
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
                            // TODO: wire to DeepSeekClient test call
                            repo.value.setState(acct.id, "idle")
                        },
                        onDelete = { acct -> repo.value.remove(acct.id) }
                    )
                    Screen.AddAccount -> placeholder("إضافة حساب")
                    Screen.Models -> placeholder("النماذج")
                    Screen.ApiConfig -> placeholder("إعدادات API")
                    Screen.ServerControl -> ServerControlScreen(
                        onBack = { pop() },
                        server = server,
                        isRunning = isRunning,
                        onToggle = {
                            if (!isRunning) {
                                server = KtorServer(8080)
                                server?.start()
                                isRunning = true
                            } else {
                                server?.stop()
                                server = null
                                isRunning = false
                            }
                        }
                    )
                    Screen.Statistics -> placeholder("الإحصائيات")
                    Screen.Logs -> placeholder("السجلات")
                    Screen.Settings -> placeholder("الإعدادات")
                }
            }
        }
    }
}

@Composable
private fun placeholder(title: String) {
    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.foundation.layout.fillMaxSize(),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(text = "شاشة $title — قيد الاستكمال", style = MaterialTheme.typography.bodyLarge)
    }
}
