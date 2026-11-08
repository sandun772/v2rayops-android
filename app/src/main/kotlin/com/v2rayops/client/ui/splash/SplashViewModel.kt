package com.v2rayops.client.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2rayops.client.domain.repository.AuthRepository
import com.v2rayops.client.domain.repository.ServerRepository
import com.v2rayops.client.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
        viewModelScope.launch(Dispatchers.IO) {
            val code = authRepository.getSavedCustomerCode() ?: "267968"
            // Dev & Offline mode: automatically sync servers & configs in background
            launch {
                try {
                    serverRepository.refreshServers()
                } catch (_: Exception) {}
            }
            launch {
                try {
                    subscriptionRepository.syncConfigs(code)
                } catch (_: Exception) {}
            }
            _target.value = SplashNavigationTarget.Dashboard
        }
    }
}
