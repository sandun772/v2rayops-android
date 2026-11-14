package com.v2rayops.client.network

import android.os.Build
import com.v2rayops.client.core.dispatchers.AppDispatchers
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.data.api.V2RayOpsClientApi
import com.v2rayops.client.data.api.dto.RemoteClientSettingsPayload
import com.v2rayops.client.data.api.dto.RemoteConfigAckRequestDto
import com.v2rayops.client.data.storage.SecureCredentialStore
import com.v2rayops.client.data.storage.UserPreferencesDataStore
import com.v2rayops.client.domain.model.RoutingMode
import com.v2rayops.client.domain.model.VpnConfig
import com.v2rayops.client.domain.repository.SubscriptionRepository
import com.v2rayops.client.profiles.ProfileParser
import com.v2rayops.client.vpn.VpnManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed class RemoteSyncState {
    data object Idle : RemoteSyncState()
    data object Syncing : RemoteSyncState()
    data class Success(val version: Long, val message: String, val timestamp: Long) : RemoteSyncState()
    data class Error(val message: String) : RemoteSyncState()
}

@Singleton
class RemoteConfigSyncManager @Inject constructor(
    private val api: V2RayOpsClientApi,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val secureStore: SecureCredentialStore,
    private val subscriptionRepository: SubscriptionRepository,
    private val profileParser: ProfileParser,
    private val vpnManager: VpnManager,
    private val dispatchers: AppDispatchers
) {
    companion object {
        private const val TAG = "RemoteConfigSync"
    }

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    private val _syncState = MutableStateFlow<RemoteSyncState>(RemoteSyncState.Idle)
    val syncState: StateFlow<RemoteSyncState> = _syncState.asStateFlow()

    fun triggerSync() {
        scope.launch {
            syncRemoteSettings()
        }
    }

    suspend fun syncRemoteSettings(): Boolean = withContext(dispatchers.io) {
        val enabled = preferencesDataStore.remoteManagementEnabledFlow.first()
        if (!enabled) {
            AppLogger.d(TAG, "Remote management is disabled by user in Settings. Skipping sync.")
            return@withContext false
        }

        val customerCode = secureStore.getCustomerCode()
        if (customerCode.isNullOrBlank()) {
            AppLogger.d(TAG, "No customer code available for remote sync. Skipping.")
            return@withContext false
        }

        val localVersion = preferencesDataStore.remoteSettingsVersionFlow.first()
        val deviceId = "${Build.MANUFACTURER}_${Build.MODEL}".replace(" ", "_").lowercase()

        _syncState.value = RemoteSyncState.Syncing
        AppLogger.i(TAG, "Checking remote settings revision (current: v$localVersion, code: $customerCode, device: $deviceId)...")

        try {
            val response = api.getRemoteSettings(
                customerCode = customerCode,
                deviceId = deviceId,
                currentVersion = localVersion
            )

            if (!response.isSuccessful || response.body() == null) {
                val err = "HTTP ${response.code()}: ${response.message()}"
                AppLogger.w(TAG, "Failed to query remote settings: $err")
                _syncState.value = RemoteSyncState.Error(err)
                return@withContext false
            }

            val body = response.body()!!
            if (!body.hasUpdate || body.version <= localVersion) {
                AppLogger.i(TAG, "Client settings are up-to-date with cloud (revision: v$localVersion)")
                _syncState.value = RemoteSyncState.Success(
                    version = localVersion,
                    message = "Up to date",
                    timestamp = System.currentTimeMillis()
                )
                return@withContext true
            }

            AppLogger.i(TAG, "New remote configuration revision detected: v${body.version} (current: v$localVersion)")

            // 1. Apply Settings Payload
            body.settings?.let { payload ->
                applySettingsPayload(payload)
            }

            // 2. Apply VLESS URL Override if present
            var updatedConfig: VpnConfig? = null
            if (!body.vlessUrl.isNullOrBlank()) {
                val parsed = profileParser.parseUri(body.vlessUrl)
                if (parsed != null) {
                    val newConfig = VpnConfig(
                        id = body.activeConfigId ?: System.currentTimeMillis(),
                        protocol = parsed.protocol.ifBlank { "vless" },
                        configUri = body.vlessUrl,
                        serverId = 11L,
                        status = "active",
                        configName = parsed.remark.ifBlank { "Remote Cloud Config" },
                        remark = parsed.remark.ifBlank { "Remote Cloud Config" },
                        host = parsed.host,
                        port = parsed.port,
                        sni = parsed.sni,
                        wsHost = parsed.wsHost,
                        wsPath = parsed.wsPath
                    )
                    subscriptionRepository.selectConfig(newConfig)
                    updatedConfig = newConfig
                    AppLogger.i(TAG, "Remote VLESS URL override applied: ${newConfig.displayName} (${newConfig.displayEndpoint})")
                } else {
                    AppLogger.w(TAG, "Could not parse remote VLESS URL: ${body.vlessUrl}")
                }
            } else if (body.activeConfigId != null) {
                val existing = subscriptionRepository.observeConfigs().first().find { it.id == body.activeConfigId }
                if (existing != null) {
                    subscriptionRepository.selectConfig(existing)
                    updatedConfig = existing
                    AppLogger.i(TAG, "Remote active config ID selected: ${existing.displayName}")
                }
            }

            // 3. Hot-Reload Running VPN Tunnel if active
            vpnManager.hotReloadTunnel(updatedConfig)

            // 4. Send ACK back to server
            val nowFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            val ackReq = RemoteConfigAckRequestDto(
                code = customerCode,
                deviceId = deviceId,
                version = body.version,
                status = "APPLIED",
                appliedAt = nowFmt,
                activeConfigName = updatedConfig?.displayName ?: "Current Profile"
            )
            val ackResp = api.acknowledgeRemoteSettings(ackReq)
            if (ackResp.isSuccessful && ackResp.body()?.ok == true) {
                AppLogger.i(TAG, "Remote revision v${body.version} acknowledged to server successfully.")
            }

            // 5. Update local revision marker
            preferencesDataStore.recordRemoteSync(body.version, "Applied revision v${body.version}")
            _syncState.value = RemoteSyncState.Success(
                version = body.version,
                message = "Revision v${body.version} applied",
                timestamp = System.currentTimeMillis()
            )
            return@withContext true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error synchronizing remote settings", e)
            _syncState.value = RemoteSyncState.Error(e.localizedMessage ?: "Network error during sync")
            return@withContext false
        }
    }

    private suspend fun applySettingsPayload(payload: RemoteClientSettingsPayload) {
        payload.routingMode?.let { modeStr ->
            try {
                val mode = RoutingMode.valueOf(modeStr.uppercase())
                preferencesDataStore.setRoutingMode(mode)
                AppLogger.d(TAG, "Applied remote routing mode: $mode")
            } catch (_: Exception) {}
        }

        payload.customDns?.let { dns ->
            if (dns.isNotBlank()) {
                preferencesDataStore.setCustomDns(dns)
                AppLogger.d(TAG, "Applied remote custom DNS: $dns")
            }
        }

        payload.smartQoSEnabled?.let {
            preferencesDataStore.setSmartQoSEnabled(it)
            AppLogger.d(TAG, "Applied remote smart QoS: $it")
        }

        payload.gamingModeEnabled?.let {
            preferencesDataStore.setGamingModeEnabled(it)
            AppLogger.d(TAG, "Applied remote gaming mode: $it")
        }

        payload.cellularMtu?.let { mtu ->
            if (mtu in 1200..1500) {
                preferencesDataStore.setCellularMtuOptimized(true)
                AppLogger.d(TAG, "Applied remote MTU: $mtu")
            }
        }

        payload.tcpBufferSizeKb?.let { kb ->
            if (kb in 16..4096) {
                preferencesDataStore.setTcpBufferSizeKb(kb)
                AppLogger.d(TAG, "Applied remote TCP buffer: ${kb}KB")
            }
        }

        payload.speedLimitMbps?.let { limit ->
            preferencesDataStore.setDownloadSpeedLimitMbps(limit)
            AppLogger.d(TAG, "Applied remote download speed limit: ${limit}Mbps")
        }

        payload.autoConnectWifi?.let {
            preferencesDataStore.setAutoConnectWifi(it)
        }

        payload.autoConnectCellular?.let {
            preferencesDataStore.setAutoConnectCellular(it)
        }

        payload.killSwitchEnabled?.let {
            preferencesDataStore.setKillSwitchEnabled(it)
        }
    }
}
