package com.deepseekweb2api.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-memory multi-account manager.
 * Tracks per-account state (idle / busy / error) and provides
 * round-robin-aware selection for the DeepSeek client.
 */
class AccountRepository {
    private val _accounts = MutableStateFlow(emptyList<Account>())
    val accounts: StateFlow<List<Account>> = _accounts

    fun add(account: Account) {
        _accounts.value = _accounts.value + account
    }

    fun remove(id: String) {
        _accounts.value = _accounts.value.filter { it.id != id }
    }

    fun update(id: String, block: Account.Builder.() -> Unit) {
        val existing = _accounts.value.firstOrNull { it.id == id } ?: return
        val builder = Account.Builder(existing)
        block(builder)
        val updated = builder.build()
        _accounts.value = _accounts.value.map { acct ->
            if (acct.id == id) updated else acct
        }
    }

    fun setState(id: String, state: String, error: String? = null) {
        _accounts.value = _accounts.value.map { acct ->
            if (acct.id != id) {
                acct
            } else if (state == "error") {
                acct.copy(
                    state = state,
                    errorCount = acct.errorCount + 1,
                    lastError = error,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                acct.copy(state = state, lastError = null, updatedAt = System.currentTimeMillis())
            }
        }
    }

    fun markRecovered(id: String) {
        _accounts.value = _accounts.value.map {
            if (it.id == id) it.copy(state = "idle", errorCount = 0, lastError = null) else it
        }
    }

    fun nextIdleAccount(): Account? =
        _accounts.value.firstOrNull { it.state == "idle" }

    fun firstAccount(): Account? = _accounts.value.firstOrNull()

    val stats: AccountStats
        get() {
            val list = _accounts.value
            return AccountStats(
                total = list.size,
                idle = list.count { it.state == "idle" },
                busy = list.count { it.state == "busy" },
                error = list.count { it.state == "error" }
            )
        }
}

data class AccountStats(
    val total: Int,
    val idle: Int,
    val busy: Int,
    val error: Int
)
