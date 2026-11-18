package com.deepseekweb2api.app.data

data class Account(
    val id: String,
    val email: String? = null,
    val token: String = "",           // stored encrypted at rest
    val cookies: String? = null,       // stored encrypted at rest
    val state: String = "idle",        // idle | busy | error
    val errorCount: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    class Builder(private val base: Account?) {
        private var id: String = base?.id ?: java.util.UUID.randomUUID().toString()
        private var email: String? = base?.email
        private var token: String = base?.token ?: ""
        private var cookies: String? = base?.cookies
        private var state: String = base?.state ?: "idle"

        fun id(value: String) = apply { id = value }
        fun email(value: String?) = apply { email = value }
        fun token(value: String) = apply { token = value }
        fun cookies(value: String?) = apply { cookies = value }
        fun state(value: String) = apply { state = value }

        fun build() = Account(id, email, token, cookies, state)
    }
}
