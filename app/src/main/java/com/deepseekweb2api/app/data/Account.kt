package com.deepseekweb2api.app.data

data class Account(
    val id: String,
    val email: String? = null,
    val token: String = "",           // stored encrypted at rest
    val cookies: String? = null,       // stored encrypted at rest
    var state: String = "idle",        // idle | busy | error
    var errorCount: Int = 0,
    var lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)
