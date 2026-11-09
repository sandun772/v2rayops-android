package com.v2rayops.client.domain

import com.v2rayops.client.core.error.AppError
import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.domain.model.UserProfile
import com.v2rayops.client.domain.repository.AuthDeepLinkPayload
import com.v2rayops.client.domain.repository.AuthRepository
import com.v2rayops.client.domain.repository.ServerRepository
import com.v2rayops.client.domain.repository.SubscriptionRepository
import com.v2rayops.client.domain.usecase.LoginWithDeepLinkUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoginWithDeepLinkUseCaseTest {

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val serverRepository: ServerRepository = mockk(relaxed = true)
    private val subscriptionRepository: SubscriptionRepository = mockk(relaxed = true)

    private lateinit var useCase: LoginWithDeepLinkUseCase

    @Before
    fun setUp() {
        useCase = LoginWithDeepLinkUseCase(authRepository, serverRepository, subscriptionRepository)
    }

    @Test
    fun invoke_whenDeepLinkPayloadSucceeds_preSyncsServersAndSubscription() = runTest {
        val payload = AuthDeepLinkPayload(
            customerCode = "267968",
            token = "jwt-session-token-abc",
            email = "user@v2rayops.com",
            name = "Test Web User"
        )
        val dummyProfile = UserProfile(
            customerCode = "267968",
            name = "Test Web User",
            email = "user@v2rayops.com",
            status = "active",
            billingMode = "payg",
            walletBalanceLkr = 1000.0,
            activeSubscriptions = emptyList(),
            trial = null
        )

        coEvery { authRepository.loginWithDeepLink(payload) } returns AppResult.Success(dummyProfile)

        val result = useCase(payload)

        assertTrue(result.isSuccess)
        assertEquals("267968", (result as AppResult.Success).data.customerCode)

        coVerify(exactly = 1) { serverRepository.refreshServers() }
        coVerify(exactly = 1) { subscriptionRepository.syncConfigs("267968") }
    }

    @Test
    fun invoke_whenDeepLinkPayloadFails_doesNotTriggerPreSync() = runTest {
        val payload = AuthDeepLinkPayload(
            customerCode = "",
            token = null,
            email = null,
            name = null
        )
        val error = AppError.Authentication("Customer code is missing")
        coEvery { authRepository.loginWithDeepLink(payload) } returns AppResult.Failure(error)

        val result = useCase(payload)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { serverRepository.refreshServers() }
        coVerify(exactly = 0) { subscriptionRepository.syncConfigs(any()) }
    }
}
