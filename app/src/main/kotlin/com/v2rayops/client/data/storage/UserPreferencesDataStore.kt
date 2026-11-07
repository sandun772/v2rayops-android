package com.v2rayops.client.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.v2rayops.client.domain.model.RoutingMode
import com.v2rayops.client.domain.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "v2rayops_user_settings")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val KILL_SWITCH = booleanPreferencesKey("kill_switch_enabled")
        val AUTO_CONNECT_WIFI = booleanPreferencesKey("auto_connect_wifi")
        val AUTO_CONNECT_CELLULAR = booleanPreferencesKey("auto_connect_cellular")
        val ROUTING_MODE = stringPreferencesKey("routing_mode")
        val CUSTOM_DNS = stringPreferencesKey("custom_dns")
        val PER_APP_PROXY = booleanPreferencesKey("per_app_proxy_enabled")
        val SELECTED_SERVER_ID = longPreferencesKey("selected_server_id")
        val PREFERRED_PROTOCOL = stringPreferencesKey("preferred_protocol")
        val SMART_QOS = booleanPreferencesKey("smart_qos_enabled")
        val SPEED_LIMIT_MBPS = intPreferencesKey("download_speed_limit_mbps")
        val CELLULAR_MTU = booleanPreferencesKey("cellular_mtu_optimized")
        val TCP_BUFFER_SIZE_KB = intPreferencesKey("tcp_buffer_size_kb")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        val routingModeString = preferences[PreferencesKeys.ROUTING_MODE] ?: RoutingMode.GLOBAL.name
        val routingMode = try {
            RoutingMode.valueOf(routingModeString)
        } catch (_: Exception) {
            RoutingMode.GLOBAL
        }

        UserSettings(
            killSwitchEnabled = preferences[PreferencesKeys.KILL_SWITCH] ?: false,
            autoConnectOnWifi = preferences[PreferencesKeys.AUTO_CONNECT_WIFI] ?: false,
            autoConnectOnCellular = preferences[PreferencesKeys.AUTO_CONNECT_CELLULAR] ?: false,
            routingMode = routingMode,
            customDns = preferences[PreferencesKeys.CUSTOM_DNS] ?: "1.1.1.1",
            perAppProxyEnabled = preferences[PreferencesKeys.PER_APP_PROXY] ?: false,
            selectedServerId = preferences[PreferencesKeys.SELECTED_SERVER_ID],
            preferredProtocol = preferences[PreferencesKeys.PREFERRED_PROTOCOL] ?: "vless",
            smartQoSEnabled = preferences[PreferencesKeys.SMART_QOS] ?: true,
            downloadSpeedLimitMbps = preferences[PreferencesKeys.SPEED_LIMIT_MBPS] ?: 0,
            cellularMtuOptimized = preferences[PreferencesKeys.CELLULAR_MTU] ?: true,
            tcpBufferSizeKb = preferences[PreferencesKeys.TCP_BUFFER_SIZE_KB] ?: 32
        )
    }

    suspend fun setKillSwitchEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.KILL_SWITCH] = enabled }
    }

    suspend fun setAutoConnectWifi(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_CONNECT_WIFI] = enabled }
    }

    suspend fun setAutoConnectCellular(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_CONNECT_CELLULAR] = enabled }
    }

    suspend fun setRoutingMode(mode: RoutingMode) {
        context.dataStore.edit { it[PreferencesKeys.ROUTING_MODE] = mode.name }
    }

    suspend fun setCustomDns(dns: String) {
        context.dataStore.edit { it[PreferencesKeys.CUSTOM_DNS] = dns }
    }

    suspend fun setSelectedServerId(serverId: Long) {
        context.dataStore.edit { it[PreferencesKeys.SELECTED_SERVER_ID] = serverId }
    }

    suspend fun setPreferredProtocol(protocol: String) {
        context.dataStore.edit { it[PreferencesKeys.PREFERRED_PROTOCOL] = protocol }
    }

    suspend fun setSmartQoSEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SMART_QOS] = enabled }
    }

    suspend fun setDownloadSpeedLimitMbps(limitMbps: Int) {
        context.dataStore.edit { it[PreferencesKeys.SPEED_LIMIT_MBPS] = limitMbps }
    }

    suspend fun setCellularMtuOptimized(optimized: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.CELLULAR_MTU] = optimized }
    }

    suspend fun setTcpBufferSizeKb(sizeKb: Int) {
        context.dataStore.edit { it[PreferencesKeys.TCP_BUFFER_SIZE_KB] = sizeKb }
    }
}
