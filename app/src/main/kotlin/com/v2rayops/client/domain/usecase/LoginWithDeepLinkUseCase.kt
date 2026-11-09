package com.v2rayops.client.domain.usecase

import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.domain.model.UserProfile
import com.v2rayops.client.domain.repository.AuthDeepLinkPayload
import com.v2rayops.client.domain.repository.AuthRepository
import com.v2rayops.client.domain.repository.ServerRepository
import com.v2rayops.client.domain.repository.SubscriptionRepository
import javax.inject.Inject

class LoginWithDeepLinkUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val serverRepository: ServerRepository,
    private val subscriptionRepository: SubscriptionRepository
) {
    suspend operator fun invoke(payload: AuthDeepLinkPayload): AppResult<UserProfile> {
        val result = authRepository.loginWithDeepLink(payload)
        if (result is AppResult.Success) {
            // Pre-sync servers and configurations upon successful authorization
            try {
                serverRepository.refreshServers()
            } catch (_: Exception) {}
            try {
                subscriptionRepository.syncConfigs(payload.customerCode)
            } catch (_: Exception) {}
        }
        return result
    }
}
