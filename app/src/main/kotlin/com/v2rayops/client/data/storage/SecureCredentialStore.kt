package com.v2rayops.client.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.v2rayops.client.core.logger.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureCredentialStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_FILE = "v2rayops_secure_prefs"
        private const val KEY_CUSTOMER_CODE = "secure_customer_code"
        private const val KEY_AUTH_TOKEN = "secure_auth_token"
        private const val KEY_USER_EMAIL = "secure_user_email"
        private const val KEY_CACHED_CONFIGS = "cached_configs_json"
        private const val KEY_CACHED_PROFILE = "cached_profile_json"
        private const val KEY_SELECTED_CONFIG_ID = "selected_config_id"
    }

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            AppLogger.e("SecureCredentialStore", "Failed to initialize EncryptedSharedPreferences, resetting", e)
            context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        }
    }

    fun saveCustomerCode(code: String) {
        prefs.edit().putString(KEY_CUSTOMER_CODE, code.trim()).apply()
    }

    fun getCustomerCode(): String? {
        return prefs.getString(KEY_CUSTOMER_CODE, null)
    }

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token.trim()).apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun saveUserEmail(email: String) {
        prefs.edit().putString(KEY_USER_EMAIL, email.trim()).apply()
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun saveCachedConfigsJson(json: String) {
        prefs.edit().putString(KEY_CACHED_CONFIGS, json).apply()
    }

    fun getCachedConfigsJson(): String? {
        return prefs.getString(KEY_CACHED_CONFIGS, null)
    }

    fun saveCachedProfileJson(json: String) {
        prefs.edit().putString(KEY_CACHED_PROFILE, json).apply()
    }

    fun getCachedProfileJson(): String? {
        return prefs.getString(KEY_CACHED_PROFILE, null)
    }

    fun saveSelectedConfigId(id: Long) {
        prefs.edit().putLong(KEY_SELECTED_CONFIG_ID, id).apply()
    }

    fun getSelectedConfigId(): Long? {
        return if (prefs.contains(KEY_SELECTED_CONFIG_ID)) prefs.getLong(KEY_SELECTED_CONFIG_ID, -1L) else null
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
