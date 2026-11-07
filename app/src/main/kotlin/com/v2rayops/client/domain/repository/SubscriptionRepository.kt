package com.v2rayops.client.domain.repository

import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.domain.model.VpnConfig
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun observeConfigs(): Flow<List<VpnConfig>>
    fun observeSelectedConfig(): Flow<VpnConfig?>
    fun selectConfig(config: VpnConfig)
    fun importManualConfig(rawUri: String): AppResult<VpnConfig>
    fun deleteConfig(configId: Long)
    suspend fun syncConfigs(customerCode: String): AppResult<List<VpnConfig>>
    suspend fun sendHeartbeat(uploadBytes: Long, downloadBytes: Long, deviceName: String?): AppResult<Boolean>
}
