package com.v2rayops.client.domain.repository

import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getSavedCustomerCode(): String?
    fun observeIsAuthenticated(): Flow<Boolean>
    suspend fun loginWithCustomerCode(customerCode: String): AppResult<UserProfile>
    suspend fun fetchProfile(): AppResult<UserProfile>
    suspend fun logout(): AppResult<Unit>
}
