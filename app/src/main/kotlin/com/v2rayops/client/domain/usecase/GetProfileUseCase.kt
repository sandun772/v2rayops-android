package com.v2rayops.client.domain.usecase

import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.domain.model.UserProfile
import com.v2rayops.client.domain.repository.AuthRepository
import javax.inject.Inject

class GetProfileUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): AppResult<UserProfile> {
        return authRepository.fetchProfile()
    }
}
