package com.v2rayops.client.domain.repository

import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

data class AuthDeepLinkPayload(
    val customerCode: String,
    val token: String? = null,
    val email: String? = null,
    val name: String? = null
)

interface AuthRepository {
    fun getSavedCustomerCode(): String?
    fun getSavedAuthToken(): String?
    fun observeIsAuthenticated(): Flow<Boolean>
    suspend fun loginWithCustomerCode(customerCode: String): AppResult<UserProfile>
    suspend fun loginWithDeepLink(payload: AuthDeepLinkPayload): AppResult<UserProfile>
    suspend fun fetchProfile(): AppResult<UserProfile>
    suspend fun logout(): AppResult<Unit>
}
