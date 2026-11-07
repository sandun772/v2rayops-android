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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
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

    override fun observeConfigs(): Flow<List<VpnConfig>> = _configs.asStateFlow()

    override fun observeSelectedConfig(): Flow<VpnConfig?> = _selectedConfig.asStateFlow()

    override fun selectConfig(config: VpnConfig) {
        _selectedConfig.value = config
        AppLogger.i("SubscriptionRepository", "Selected active config: ${config.displayName}")
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
                            serverFlag = dto.serverFlag ?: "🇸🇬"
                        )
                    }

                    _configs.value = parsedConfigs

                    // Auto-select first active config if none currently selected or if stale
                    val currentSelected = _selectedConfig.value
                    if (currentSelected == null || parsedConfigs.none { it.id == currentSelected.id }) {
                        _selectedConfig.value = parsedConfigs.firstOrNull()
                    }

                    AppLogger.i("SubscriptionRepository", "Synced ${parsedConfigs.size} active configs for customer $customerCode")
                    AppResult.Success(parsedConfigs)
                } else {
                    AppResult.Failure(AppError.Server(response.code(), response.message()))
                }
            } catch (e: Exception) {
                AppLogger.e("SubscriptionRepository", "Failed to sync configs", e)
                AppResult.Failure(AppError.Network("Failed to retrieve subscription configs", e))
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
