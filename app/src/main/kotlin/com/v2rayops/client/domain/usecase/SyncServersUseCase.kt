package com.v2rayops.client.domain.usecase

import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.domain.model.ServerNode
import com.v2rayops.client.domain.repository.ServerRepository
import javax.inject.Inject

class SyncServersUseCase @Inject constructor(
    private val serverRepository: ServerRepository
) {
    suspend operator fun invoke(): AppResult<List<ServerNode>> {
        return serverRepository.refreshServers()
    }
}
