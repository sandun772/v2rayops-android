package com.v2rayops.client.domain.usecase

import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.domain.model.UserProfile
import com.v2rayops.client.domain.repository.AuthRepository
import com.v2rayops.client.domain.repository.ServerRepository
import com.v2rayops.client.domain.repository.SubscriptionRepository
import javax.inject.Inject

class LoginWithCodeUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val serverRepository: ServerRepository,
    private val subscriptionRepository: SubscriptionRepository
) {
    suspend operator fun invoke(customerCode: String): AppResult<UserProfile> {
        val result = authRepository.loginWithCustomerCode(customerCode)
        if (result is AppResult.Success) {
            // Pre-sync servers and configurations upon successful login
            serverRepository.refreshServers()
            subscriptionRepository.syncConfigs(customerCode)
        }
        return result
    }
}
