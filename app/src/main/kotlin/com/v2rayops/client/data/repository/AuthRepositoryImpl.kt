package com.v2rayops.client.data.repository

import com.v2rayops.client.core.dispatchers.AppDispatchers
import com.v2rayops.client.core.error.AppError
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.data.api.V2RayOpsClientApi
import com.v2rayops.client.data.api.dto.ProfileResponseDto
import com.v2rayops.client.data.storage.SecureCredentialStore
import com.v2rayops.client.domain.model.TrialInfo
import com.v2rayops.client.domain.model.UserProfile
import com.v2rayops.client.domain.model.UserSubscription
import com.v2rayops.client.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: V2RayOpsClientApi,
    private val secureStore: SecureCredentialStore,
    private val dispatchers: AppDispatchers
) : AuthRepository {

    private val _isAuthenticated = MutableStateFlow(!secureStore.getCustomerCode().isNullOrBlank())

    override fun getSavedCustomerCode(): String? = secureStore.getCustomerCode()

    override fun observeIsAuthenticated(): Flow<Boolean> = _isAuthenticated.asStateFlow()

    override suspend fun loginWithCustomerCode(customerCode: String): AppResult<UserProfile> =
        withContext(dispatchers.io) {
            val cleanCode = customerCode.trim()
            if (cleanCode.isBlank()) {
                return@withContext AppResult.Failure(AppError.Authentication("Customer code cannot be empty"))
            }

            try {
                val response = api.getProfile(cleanCode)
                if (response.isSuccessful && response.body() != null) {
                    val dto = response.body()!!
                    val profile = mapProfile(dto)

                    if (profile.status != "active") {
                        return@withContext AppResult.Failure(
                            AppError.SubscriptionExpired("Your account is currently ${profile.status}. Please contact support or renew your package.")
                        )
                    }

                    secureStore.saveCustomerCode(cleanCode)
                    profile.email.let { secureStore.saveUserEmail(it) }
                    _isAuthenticated.value = true

                    AppResult.Success(profile)
                } else {
                    val code = response.code()
                    if (code == 404) {
                        AppResult.Failure(AppError.Authentication("Customer code not found. Please check and try again."))
                    } else if (code == 403) {
                        AppResult.Failure(AppError.SubscriptionExpired("Account is suspended or inactive."))
                    } else {
                        AppResult.Failure(AppError.Server(code, "Server error (${response.message()})"))
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("AuthRepository", "Failed to login with customer code", e)
                AppResult.Failure(AppError.Network("Cannot connect to V2RayOps server. Check your connection.", e))
            }
        }

    override suspend fun fetchProfile(): AppResult<UserProfile> = withContext(dispatchers.io) {
        val code = secureStore.getCustomerCode()
            ?: return@withContext AppResult.Failure(AppError.Authentication("No customer code saved. Please log in."))

        try {
            val response = api.getProfile(code)
            if (response.isSuccessful && response.body() != null) {
                val profile = mapProfile(response.body()!!)
                AppResult.Success(profile)
            } else {
                AppResult.Failure(AppError.Server(response.code(), response.message()))
            }
        } catch (e: Exception) {
            AppLogger.d("AuthRepository", "Failed to fetch remote profile (${e.message}), continuing with cached credentials")
            AppResult.Failure(AppError.Network("Failed to refresh profile", e))
        }
    }

    override suspend fun logout(): AppResult<Unit> = withContext(dispatchers.io) {
        secureStore.clear()
        _isAuthenticated.value = false
        AppResult.Success(Unit)
    }

    private fun mapProfile(dto: ProfileResponseDto): UserProfile {
        return UserProfile(
            customerCode = dto.user.customerCode,
            name = dto.user.name ?: "V2RayOps User",
            email = dto.user.email ?: "",
            status = dto.user.status,
            billingMode = dto.user.billingMode ?: "payg",
            walletBalanceLkr = dto.wallet.balanceLkr ?: 0.0,
            activeSubscriptions = dto.subscriptions.map { sub ->
                UserSubscription(
                    id = sub.id,
                    planName = sub.planName ?: "Standard Plan",
                    status = sub.status,
                    dataGb = sub.dataGb ?: 0.0,
                    startDate = sub.startDate,
                    expiryDate = sub.expiryDate
                )
            },
            trial = dto.trial?.let { t ->
                TrialInfo(
                    dataGb = t.dataGb ?: 0.0,
                    durationHours = t.durationHours ?: 0,
                    status = t.status,
                    startedAt = t.startedAt,
                    expiresAt = t.expiresAt,
                    usedBytes = t.usedBytes ?: 0
                )
            }
        )
    }
}
