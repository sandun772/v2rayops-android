package com.v2rayops.client.domain.repository

import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.domain.model.ServerNode
import kotlinx.coroutines.flow.Flow

interface ServerRepository {
    fun observeServers(): Flow<List<ServerNode>>
    fun observeSelectedServer(): Flow<ServerNode?>
    suspend fun refreshServers(): AppResult<List<ServerNode>>
    suspend fun selectServer(serverId: Long)
    suspend fun testLatency(server: ServerNode): Long?
}
