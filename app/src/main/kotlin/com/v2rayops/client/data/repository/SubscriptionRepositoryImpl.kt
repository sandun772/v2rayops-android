package com.v2rayops.client.data.repository

import com.v2rayops.client.core.dispatchers.AppDispatchers
import com.v2rayops.client.core.error.AppError
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.data.api.V2RayOpsClientApi
import com.v2rayops.client.data.api.dto.HeartbeatRequestDto
import com.v2rayops.client.data.storage.SecureCredentialStore
import com.v2rayops.client.domain.model.VpnConfig
import com.v2rayops.client.domain.repository.SubscriptionRepository
import com.v2rayops.client.profiles.ProfileParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val api: V2RayOpsClientApi,
    private val secureStore: SecureCredentialStore,
    private val profileParser: ProfileParser,
    private val dispatchers: AppDispatchers
) : SubscriptionRepository {

    private val _configs = MutableStateFlow<List<VpnConfig>>(emptyList())
    private val _selectedConfig = MutableStateFlow<VpnConfig?>(null)

    init {
        CoroutineScope(dispatchers.io).launch {
            loadCachedConfigs()
        }
    }

    private fun loadCachedConfigs() {
        try {
            val jsonStr = secureStore.getCachedConfigsJson() ?: return
            val array = JSONArray(jsonStr)
            val list = mutableListOf<VpnConfig>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rawUri = obj.optString("configUri", "")
                val parsed = profileParser.parseUri(rawUri)

                list.add(
                    VpnConfig(
                        id = obj.optLong("id"),
                        protocol = obj.optString("protocol", "vless"),
                        configUri = rawUri,
                        serverId = obj.optLong("serverId"),
                        status = obj.optString("status", "active"),
                        configName = obj.optString("configName", ""),
                        remark = obj.optString("remark", parsed?.remark ?: "V2RayOps Config"),
                        host = obj.optString("host", parsed?.host ?: ""),
                        port = obj.optInt("port", parsed?.port ?: 443),
                        expiresAt = if (obj.has("expiresAt") && !obj.isNull("expiresAt")) obj.getString("expiresAt") else null,
                        serverName = obj.optString("serverName", "V2RayOps Singapore"),
                        serverCountry = obj.optString("serverCountry", "SG"),
                        serverFlag = obj.optString("serverFlag", "SG"),
                        sni = obj.optString("sni", parsed?.sni ?: ""),
                        wsHost = obj.optString("wsHost", parsed?.wsHost ?: ""),
                        wsPath = obj.optString("wsPath", parsed?.wsPath ?: ""),
                        tuningMode = if (obj.has("tuningMode") && !obj.isNull("tuningMode")) obj.getString("tuningMode") else null,
                        customMtu = if (obj.has("customMtu") && !obj.isNull("customMtu")) obj.getInt("customMtu") else null,
                        customTcpBufferKb = if (obj.has("customTcpBufferKb") && !obj.isNull("customTcpBufferKb")) obj.getInt("customTcpBufferKb") else null,
                        customDns = if (obj.has("customDns") && !obj.isNull("customDns")) obj.getString("customDns") else null,
                        dpiDelayFix = if (obj.has("dpiDelayFix") && !obj.isNull("dpiDelayFix")) obj.getBoolean("dpiDelayFix") else null,
                        smartQoS = if (obj.has("smartQoS") && !obj.isNull("smartQoS")) obj.getBoolean("smartQoS") else null,
                        measuredSpeedMbps = if (obj.has("measuredSpeedMbps") && !obj.isNull("measuredSpeedMbps")) obj.getInt("measuredSpeedMbps") else null,
                        measuredUploadMbps = if (obj.has("measuredUploadMbps") && !obj.isNull("measuredUploadMbps")) obj.getDouble("measuredUploadMbps") else null,
                        measuredPingMs = if (obj.has("measuredPingMs") && !obj.isNull("measuredPingMs")) obj.getLong("measuredPingMs") else null,
                        stabilityScore = if (obj.has("stabilityScore") && !obj.isNull("stabilityScore")) obj.getInt("stabilityScore") else null
                    )
                )
            }

            if (list.isNotEmpty()) {
                _configs.value = list
                val savedId = secureStore.getSelectedConfigId()
                val matched = list.find { it.id == savedId } ?: list.first()
                _selectedConfig.value = matched
                AppLogger.i("SubscriptionRepository", "Loaded ${list.size} offline cached configs from persistent storage (selected: ${matched.displayName})")
            }
        } catch (e: Exception) {
            AppLogger.w("SubscriptionRepository", "Failed to deserialize cached configs: ${e.message}")
        }
    }

    private fun saveConfigsToCache(configs: List<VpnConfig>) {
        try {
            val array = JSONArray()
            for (c in configs) {
                val obj = JSONObject().apply {
                    put("id", c.id)
                    put("protocol", c.protocol)
                    put("configUri", c.configUri)
                    put("serverId", c.serverId)
                    put("status", c.status)
                    put("configName", c.configName)
                    put("remark", c.remark)
                    put("host", c.host)
                    put("port", c.port)
                    put("expiresAt", c.expiresAt)
                    put("serverName", c.serverName)
                    put("serverCountry", c.serverCountry)
                    put("serverFlag", c.serverFlag)
                    put("sni", c.sni)
                    put("wsHost", c.wsHost)
                    put("wsPath", c.wsPath)
                    c.tuningMode?.let { put("tuningMode", it) }
                    c.customMtu?.let { put("customMtu", it) }
                    c.customTcpBufferKb?.let { put("customTcpBufferKb", it) }
                    c.customDns?.let { put("customDns", it) }
                    c.dpiDelayFix?.let { put("dpiDelayFix", it) }
                    c.smartQoS?.let { put("smartQoS", it) }
                    c.measuredSpeedMbps?.let { put("measuredSpeedMbps", it) }
                    c.measuredUploadMbps?.let { put("measuredUploadMbps", it) }
                    c.measuredPingMs?.let { put("measuredPingMs", it) }
                    c.stabilityScore?.let { put("stabilityScore", it) }
                }
                array.put(obj)
            }
            secureStore.saveCachedConfigsJson(array.toString())
        } catch (e: Exception) {
            AppLogger.w("SubscriptionRepository", "Failed to cache configs to storage: ${e.message}")
        }
    }

    override fun saveTunedPresets(presets: List<VpnConfig>) {
        if (presets.isEmpty()) return
        val currentList = _configs.value.toMutableList()
        for (preset in presets) {
            val idx = currentList.indexOfFirst { it.id == preset.id || (it.tuningMode != null && it.tuningMode == preset.tuningMode) }
            if (idx >= 0) {
                currentList[idx] = preset
            } else {
                currentList.add(preset)
            }
        }
        _configs.value = currentList
        saveConfigsToCache(currentList)
        AppLogger.i("SubscriptionRepository", "Saved ${presets.size} tuned presets to persistent configuration store")
    }

    override fun observeConfigs(): Flow<List<VpnConfig>> = _configs.asStateFlow()

    override fun observeSelectedConfig(): Flow<VpnConfig?> = _selectedConfig.asStateFlow()

    override fun selectConfig(config: VpnConfig) {
        val currentList = _configs.value
        val updatedList = if (currentList.any { it.id == config.id }) {
            currentList.map { if (it.id == config.id) config else it }
        } else {
            listOf(config) + currentList
        }
        _configs.value = updatedList
        _selectedConfig.value = config
        secureStore.saveSelectedConfigId(config.id)
        saveConfigsToCache(updatedList)
        AppLogger.i("SubscriptionRepository", "Selected active config: ${config.displayName} (addr: ${config.effectiveAddr}, sni: ${config.effectiveSni})")
    }

    override fun importManualConfig(rawUri: String): AppResult<VpnConfig> {
        val trimmed = rawUri.trim()
        if (trimmed.isBlank()) {
            return AppResult.Failure(AppError.Validation("Configuration link cannot be empty"))
        }
        val parsed = profileParser.parseUri(trimmed)
            ?: return AppResult.Failure(AppError.Validation("Invalid V2Ray link. Must start with vless://, vmess://, trojan://, or ss://"))

        val newId = System.currentTimeMillis()
        val remarkName = parsed.remark.ifBlank { "Imported ${parsed.protocol.uppercase()}" }
        val newConfig = VpnConfig(
            id = newId,
            protocol = parsed.protocol,
            configUri = trimmed,
            serverId = 0L,
            status = "active",
            configName = remarkName,
            remark = remarkName,
            host = parsed.host,
            port = parsed.port,
            expiresAt = null,
            serverName = "Custom (${parsed.host})",
            serverCountry = "SG",
            serverFlag = "CUSTOM",
            sni = parsed.sni,
            wsHost = parsed.wsHost,
            wsPath = parsed.wsPath
        )

        val current = _configs.value.toMutableList()
        current.add(0, newConfig)
        _configs.value = current
        _selectedConfig.value = newConfig
        secureStore.saveSelectedConfigId(newId)
        saveConfigsToCache(current)
        AppLogger.i("SubscriptionRepository", "Successfully imported manual config: ${newConfig.displayName} (${newConfig.displayEndpoint})")
        return AppResult.Success(newConfig)
    }

    override fun deleteConfig(configId: Long) {
        val current = _configs.value.filter { it.id != configId }
        _configs.value = current
        if (_selectedConfig.value?.id == configId) {
            _selectedConfig.value = current.firstOrNull()
            _selectedConfig.value?.let { secureStore.saveSelectedConfigId(it.id) }
        }
        saveConfigsToCache(current)
        AppLogger.i("SubscriptionRepository", "Deleted config #$configId")
    }

    override suspend fun syncConfigs(customerCode: String): AppResult<List<VpnConfig>> =
        withContext(dispatchers.io) {
            try {
                val response = api.getSubscription(customerCode, format = "json")
                if (response.isSuccessful && response.body() != null) {
                    val dtoList = response.body()!!.configs
                    val parsedConfigs = dtoList.map { dto ->
                        val rawUri = (dto.configUri ?: dto.configData ?: "").trim()
                        val parsed = profileParser.parseUri(rawUri)

                        VpnConfig(
                            id = dto.id,
                            protocol = dto.protocol.ifBlank { "vless" },
                            configUri = rawUri,
                            serverId = dto.serverId,
                            status = dto.status,
                            configName = dto.configName ?: "",
                            remark = parsed?.remark ?: dto.configName ?: "${dto.protocol.uppercase()} Config #${dto.id}",
                            host = parsed?.host ?: "",
                            port = parsed?.port ?: 443,
                            expiresAt = dto.expiresAt,
                            serverName = dto.serverName ?: "",
                            serverCountry = dto.serverCountry ?: "SG",
                            serverFlag = dto.serverFlag ?: "SG",
                            sni = parsed?.sni ?: "",
                            wsHost = parsed?.wsHost ?: "",
                            wsPath = parsed?.wsPath ?: ""
                        )
                    }

                    _configs.value = parsedConfigs
                    saveConfigsToCache(parsedConfigs)

                    // Auto-select first active config if none currently selected or if stale
                    val currentSelected = _selectedConfig.value
                    if (currentSelected == null || parsedConfigs.none { it.id == currentSelected.id }) {
                        val first = parsedConfigs.firstOrNull()
                        _selectedConfig.value = first
                        if (first != null) {
                            secureStore.saveSelectedConfigId(first.id)
                        }
                    }

                    AppLogger.i("SubscriptionRepository", "Synced ${parsedConfigs.size} active configs for customer $customerCode")
                    AppResult.Success(parsedConfigs)
                } else {
                    val cached = _configs.value
                    if (cached.isNotEmpty()) {
                        AppLogger.w("SubscriptionRepository", "Server returned ${response.code()}, falling back to ${cached.size} cached configs")
                        AppResult.Success(cached)
                    } else {
                        AppResult.Failure(AppError.Server(response.code(), response.message()))
                    }
                }
            } catch (e: Exception) {
                val cached = _configs.value
                if (cached.isNotEmpty()) {
                    AppLogger.w("SubscriptionRepository", "Network sync failed (${e.message}), gracefully using ${cached.size} cached configs for offline/zero-rated operation")
                    AppResult.Success(cached)
                } else {
                    AppLogger.e("SubscriptionRepository", "Failed to sync configs and no offline cache available", e)
                    AppResult.Failure(AppError.Network("Failed to retrieve subscription configs", e))
                }
            }
        }

    override suspend fun sendHeartbeat(
        uploadBytes: Long,
        downloadBytes: Long,
        deviceName: String?
    ): AppResult<Boolean> = withContext(dispatchers.io) {
        val code = secureStore.getCustomerCode()
            ?: return@withContext AppResult.Failure(AppError.Authentication("No active customer code for heartbeat"))

        try {
            val response = api.sendHeartbeat(
                HeartbeatRequestDto(
                    code = code,
                    uploadBytes = uploadBytes,
                    downloadBytes = downloadBytes,
                    deviceName = deviceName ?: "Android Device"
                )
            )

            if (response.isSuccessful && response.body()?.ok == true) {
                AppResult.Success(true)
            } else {
                AppResult.Failure(AppError.Server(response.code(), response.body()?.error ?: "Heartbeat failed"))
            }
        } catch (e: Exception) {
            AppLogger.d("SubscriptionRepository", "Heartbeat sync failed: ${e.message}")
            AppResult.Failure(AppError.Network("Heartbeat network error", e))
        }
    }
}
