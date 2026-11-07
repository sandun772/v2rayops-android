package com.v2rayops.client.domain.usecase

import com.v2rayops.client.core.error.AppError
import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.domain.model.VpnConfig
import com.v2rayops.client.domain.repository.AuthRepository
import com.v2rayops.client.domain.repository.SubscriptionRepository
import javax.inject.Inject

class SyncSubscriptionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val subscriptionRepository: SubscriptionRepository
) {
    suspend operator fun invoke(): AppResult<List<VpnConfig>> {
        val code = authRepository.getSavedCustomerCode()
            ?: return AppResult.Failure(AppError.Authentication("User not logged in"))

        return subscriptionRepository.syncConfigs(code)
    }
}
