package com.deepseekweb2api.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.widget.Toast
import com.deepseekweb2api.app.client.DeepSeekClient
import com.deepseekweb2api.app.data.Account
import com.deepseekweb2api.app.data.AccountRepository
import com.deepseekweb2api.app.server.LocalApiService
import com.deepseekweb2api.app.ui.screens.AccountManagerScreen
import com.deepseekweb2api.app.ui.screens.AddEditAccountScreen
import com.deepseekweb2api.app.ui.screens.ApiConfigScreen
import com.deepseekweb2api.app.ui.screens.DashboardScreen
import com.deepseekweb2api.app.ui.screens.LogsScreen
import com.deepseekweb2api.app.ui.screens.ModelSelectionScreen
import com.deepseekweb2api.app.ui.screens.ServerControlScreen
import com.deepseekweb2api.app.ui.screens.SettingsScreen
import com.deepseekweb2api.app.ui.screens.StatisticsScreen
import com.deepseekweb2api.app.ui.theme.DeepSeekTheme
import kotlinx.coroutines.launch

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

    val title: String
        get() = when (this) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val baseRepo = remember { AccountRepository() }
    var navStack by remember { mutableStateOf(listOf<Screen>(Screen.Dashboard)) }
    val screen: Screen = navStack.last()

    val accounts by baseRepo.accounts.collectAsState()
    var isRunning by remember { mutableStateOf(false) }
    var userAgent by remember { mutableStateOf(DeepSeekClient.DEFAULT_USER_AGENT) }
    var secChUa by remember { mutableStateOf(DeepSeekClient.DEFAULT_SEC_CH_UA) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun push(s: Screen) { navStack = navStack + s }
    fun pop() { if (navStack.size > 1) navStack = navStack.dropLast(1) }

    // Initialize service with accounts
    val initService = remember {
        LocalApiService.accounts = baseRepo
    }

    // Start/stop service via LocalApiService
    fun startServer() {
        context.startForegroundService(Intent(context, LocalApiService::class.java).apply {
            action = LocalApiService.ACTION_START_SERVER
        })
    }

    fun stopServer() {
        context.startForegroundService(Intent(context, LocalApiService::class.java).apply {
            action = LocalApiService.ACTION_STOP_SERVER
        })
    }

    // Sync isRunning state with service
    fun checkServerState() {
        isRunning = LocalApiService.isServerActive
    }

    DeepSeekTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(text = screen.title) },
                    navigationIcon = {
                        if (navStack.size > 1) {
                            IconButton(onClick = { pop() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "رجوع"
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (screen) {
                    is Screen.Dashboard -> DashboardScreen(
                        onAccounts = { push(Screen.Accounts) },
                        onServer = { push(Screen.ServerControl) },
                        onModels = { push(Screen.Models) },
                        onLogs = { push(Screen.Logs) },
                        onSettings = { push(Screen.Settings) },
                        stats = baseRepo.stats
                    )

                    is Screen.Accounts -> AccountManagerScreen(
                        onBack = { pop() },
                        onAdd = { push(Screen.AddAccount) },
                        accounts = accounts,
                        onTest = { acct ->
                            baseRepo.setState(acct.id, "busy")
                            scope.launch {
                                val result = runCatching {
                                    DeepSeekClient.create(acct.token, acct.cookies, userAgent, secChUa).testConnection()
                                }.getOrNull()
                                if (result != null && result.success) {
                                    baseRepo.markRecovered(acct.id)
                                    Toast.makeText(context, "تم الاتصال بنجاح (${result.httpCode})", Toast.LENGTH_SHORT).show()
                                } else {
                                    val msg = result?.exceptionMessage ?: result?.httpCodeMeaning ?: "فشل الاتصال"
                                    baseRepo.setState(acct.id, "error", msg)
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onDelete = { acct -> baseRepo.remove(acct.id) }
                    )

                    is Screen.AddAccount -> AddEditAccountScreen(
                        onBack = { pop() },
                        repo = baseRepo,
                        onSaved = { pop() },
                        userAgent = userAgent,
                        secChUa = secChUa
                    )

                    is Screen.Models -> ModelSelectionScreen(
                        onBack = { pop() },
                        onSelect = {}
                    )

                    is Screen.ApiConfig -> ApiConfigScreen(onBack = { pop() })

                    is Screen.ServerControl -> ServerControlScreen(
                        onBack = { pop() },
                        isRunning = isRunning,
                        onToggle = {
                            if (!isRunning) {
                                startServer()
                            } else {
                                stopServer()
                            }
                            // Refresh state after toggle
                            checkServerState()
                        }
                    )

                    is Screen.Statistics -> StatisticsScreen(
                        onBack = { pop() },
                        stats = baseRepo.stats
                    )

                    is Screen.Logs -> LogsScreen(onBack = { pop() })

                    is Screen.Settings -> SettingsScreen(
                        onBack = { pop() },
                        userAgent = userAgent,
                        secChUa = secChUa,
                        onUserAgentChange = { userAgent = it },
                        onSecChUaChange = { secChUa = it }
                    )
                }
            }
        }
    }
}
