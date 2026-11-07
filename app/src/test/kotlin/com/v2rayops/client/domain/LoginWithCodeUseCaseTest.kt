package com.v2rayops.client.domain

import com.v2rayops.client.core.error.AppError
import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.domain.model.UserProfile
import com.v2rayops.client.domain.repository.AuthRepository
import com.v2rayops.client.domain.repository.ServerRepository
import com.v2rayops.client.domain.repository.SubscriptionRepository
import com.v2rayops.client.domain.usecase.LoginWithCodeUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoginWithCodeUseCaseTest {

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val serverRepository: ServerRepository = mockk(relaxed = true)
    private val subscriptionRepository: SubscriptionRepository = mockk(relaxed = true)

    private lateinit var useCase: LoginWithCodeUseCase

    @Before
    fun setUp() {
        useCase = LoginWithCodeUseCase(authRepository, serverRepository, subscriptionRepository)
    }

    @Test
    fun invoke_whenLoginSucceeds_preSyncsServersAndSubscription() = runTest {
        val dummyProfile = UserProfile(
            customerCode = "V2R-123456",
            name = "Test User",
            email = "user@test.com",
            status = "active",
            billingMode = "payg",
            walletBalanceLkr = 500.0,
            activeSubscriptions = emptyList(),
            trial = null
        )

        coEvery { authRepository.loginWithCustomerCode("V2R-123456") } returns AppResult.Success(dummyProfile)

        val result = useCase("V2R-123456")

        assertTrue(result.isSuccess)
        assertEquals("V2R-123456", (result as AppResult.Success).data.customerCode)

        coVerify(exactly = 1) { serverRepository.refreshServers() }
        coVerify(exactly = 1) { subscriptionRepository.syncConfigs("V2R-123456") }
    }

    @Test
    fun invoke_whenLoginFails_doesNotTriggerPreSync() = runTest {
        val error = AppError.Authentication("Invalid code")
        coEvery { authRepository.loginWithCustomerCode("INVALID") } returns AppResult.Failure(error)

        val result = useCase("INVALID")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { serverRepository.refreshServers() }
        coVerify(exactly = 0) { subscriptionRepository.syncConfigs(any()) }
    }
}
