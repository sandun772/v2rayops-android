package com.v2rayops.client.data.repository

import com.v2rayops.client.core.dispatchers.AppDispatchers
import com.v2rayops.client.core.error.AppError
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.data.api.V2RayOpsClientApi
import com.v2rayops.client.data.storage.UserPreferencesDataStore
import com.v2rayops.client.domain.model.ServerNode
import com.v2rayops.client.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepositoryImpl @Inject constructor(
    private val api: V2RayOpsClientApi,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val dispatchers: AppDispatchers
) : ServerRepository {

    private val _servers = MutableStateFlow<List<ServerNode>>(emptyList())

    override fun observeServers(): Flow<List<ServerNode>> = _servers.asStateFlow()

    override fun observeSelectedServer(): Flow<ServerNode?> =
        combine(_servers, preferencesDataStore.userSettingsFlow) { servers, settings ->
            if (servers.isEmpty()) return@combine null
            val selectedId = settings.selectedServerId
            servers.find { it.id == selectedId } ?: servers.firstOrNull()
        }

    override suspend fun refreshServers(): AppResult<List<ServerNode>> = withContext(dispatchers.io) {
        try {
            val response = api.getServers()
            if (response.isSuccessful && response.body() != null) {
                val serverList = response.body()!!.servers.map { dto ->
                    ServerNode(
                        id = dto.id,
                        name = dto.name,
                        hostname = dto.hostname,
                        port = dto.port ?: 443
                    )
                }

                _servers.value = serverList

                // Concurrently probe latencies
                val withLatency = serverList.map { server ->
                    val latency = testLatency(server)
                    server.copy(latencyMs = latency)
                }
                _servers.value = withLatency

                AppResult.Success(withLatency)
            } else {
                AppResult.Failure(AppError.Server(response.code(), response.message()))
            }
        } catch (e: Exception) {
            AppLogger.e("ServerRepository", "Failed to fetch servers", e)
            AppResult.Failure(AppError.Network("Failed to retrieve server nodes", e))
        }
    }

    override suspend fun selectServer(serverId: Long) {
        preferencesDataStore.setSelectedServerId(serverId)
    }

    override suspend fun testLatency(server: ServerNode): Long? = withContext(dispatchers.io) {
        try {
            val startTime = System.currentTimeMillis()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(server.hostname, server.port), 3000)
            }
            val elapsed = System.currentTimeMillis() - startTime
            elapsed
        } catch (e: Exception) {
            AppLogger.d("ServerRepository", "Latency test failed for ${server.hostname}: ${e.message}")
            null
        }
    }
}
