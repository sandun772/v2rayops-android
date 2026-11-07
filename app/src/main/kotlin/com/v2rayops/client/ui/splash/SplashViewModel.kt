package com.v2rayops.client.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2rayops.client.domain.repository.AuthRepository
import com.v2rayops.client.domain.repository.ServerRepository
import com.v2rayops.client.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SplashNavigationTarget {
    data object Loading : SplashNavigationTarget
    data object Dashboard : SplashNavigationTarget
    data object Login : SplashNavigationTarget
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val serverRepository: ServerRepository,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _target = MutableStateFlow<SplashNavigationTarget>(SplashNavigationTarget.Loading)
    val target: StateFlow<SplashNavigationTarget> = _target.asStateFlow()

    init {
        checkAuthentication()
    }

    private fun checkAuthentication() {
        viewModelScope.launch {
            // Smooth minimum splash delay for branding presence
            delay(1200)

            val code = authRepository.getSavedCustomerCode()
            if (!code.isNullOrBlank()) {
                // Background warm-up
                serverRepository.refreshServers()
                subscriptionRepository.syncConfigs(code)
                _target.value = SplashNavigationTarget.Dashboard
            } else {
                _target.value = SplashNavigationTarget.Login
            }
        }
    }
}
