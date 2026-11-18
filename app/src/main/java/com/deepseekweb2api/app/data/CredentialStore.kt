package com.deepseekweb2api.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Persists DeepSeek account credentials at rest.
 * Keys are generated/stored in the Android Keystore via [MasterKeys],
 * so token and cookie values are never written to disk in plain text.
 */
object CredentialStore {
    private const val PREF_NAME = "deepseek_accounts"
    private lateinit var prefs: EncryptedSharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        prefs = EncryptedSharedPreferences.create(
            PREF_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveAccount(account: Account) {
        prefs.edit()
            .putString("token_${account.id}", account.token)
            .putString("cookies_${account.id}", account.cookies ?: "")
            .apply()
    }

    fun getToken(accountId: String): String? =
        prefs.getString("token_$accountId", null).takeIf { it.isNotEmpty() }

    fun getCookies(accountId: String): String? =
        prefs.getString("cookies_$accountId", null).takeIf { it.isNotEmpty() }

    fun removeAccount(accountId: String) {
        prefs.edit()
            .remove("token_$accountId")
            .remove("cookies_$accountId")
            .apply()
    }
}
