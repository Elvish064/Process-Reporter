package me.elvish.statusreporter.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import android.net.Uri
import android.util.Log
import me.elvish.statusreporter.service.HeartbeatWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    // --- Non-sensitive settings via DataStore ---

    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val REPORT_INTERVAL = intPreferencesKey("report_interval")
        val CUSTOM_EMOJI_MAP = stringPreferencesKey("custom_emoji_map")
        val FOREGROUND_SERVICE_ENABLED = booleanPreferencesKey("foreground_service_enabled")
        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.SERVER_URL] ?: ""
    }

    val reportInterval: Flow<Int> = context.dataStore.data.map { prefs ->
        (prefs[Keys.REPORT_INTERVAL] ?: HeartbeatWorker.DEFAULT_INTERVAL_SECONDS)
            .coerceIn(HeartbeatWorker.MIN_INTERVAL_SECONDS, HeartbeatWorker.MAX_INTERVAL_SECONDS)
    }

    val customEmojiMap: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.CUSTOM_EMOJI_MAP] ?: ""
    }

    val foregroundServiceEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.FOREGROUND_SERVICE_ENABLED] ?: false
    }

    val monitoringEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.MONITORING_ENABLED] ?: false
    }

    val lastSyncTimestamp: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_SYNC_TIMESTAMP] ?: 0L
    }

    suspend fun setServerUrl(url: String) {
        require(validateUrl(url)) { "Invalid URL: must be HTTPS or http://localhost" }
        context.dataStore.edit { it[Keys.SERVER_URL] = url.trim() }
    }

    suspend fun setReportInterval(seconds: Int) {
        context.dataStore.edit {
            it[Keys.REPORT_INTERVAL] = seconds.coerceIn(
                HeartbeatWorker.MIN_INTERVAL_SECONDS,
                HeartbeatWorker.MAX_INTERVAL_SECONDS,
            )
        }
    }

    suspend fun setCustomEmojiMap(emoji: String) {
        context.dataStore.edit { it[Keys.CUSTOM_EMOJI_MAP] = emoji }
    }

    suspend fun setForegroundServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FOREGROUND_SERVICE_ENABLED] = enabled }
    }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MONITORING_ENABLED] = enabled }
    }

    /** Update last sync timestamp with compare-and-set (only advances forward). */
    suspend fun setLastSyncTimestamp(millis: Long) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.LAST_SYNC_TIMESTAMP] ?: 0L
            if (millis > current) {
                prefs[Keys.LAST_SYNC_TIMESTAMP] = millis
            }
        }
    }

    // --- Sensitive token via EncryptedSharedPreferences ---

    private val encryptedPrefs: SharedPreferences? by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "secure_prefs",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("SettingsStore", "EncryptedSharedPreferences unavailable", e)
            null
        }
    }

    val isSecureStorageAvailable: Boolean get() = encryptedPrefs != null

    fun getToken(): String? {
        val prefs = encryptedPrefs ?: return null
        return prefs.getString("token", null)
    }

    fun setToken(token: String): Boolean {
        val prefs = encryptedPrefs ?: return false
        return prefs.edit().putString("token", token).commit()
    }

    companion object {
        fun maskToken(token: String): String {
            if (token.length <= 4) return "****"
            return token.take(4) + "***"
        }

        fun validateUrl(url: String): Boolean {
            val trimmed = url.trim()
            if (trimmed.isEmpty()) return false
            val uri = Uri.parse(trimmed)
            val scheme = uri.scheme ?: return false
            val host = uri.host ?: return false
            return when (scheme) {
                "https" -> true
                "http" -> host == "localhost" || host == "127.0.0.1"
                else -> false
            }
        }
    }
}
