package com.v2rayops.client.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2rayops.client.BuildConfig
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.domain.repository.AuthDeepLinkPayload
import com.v2rayops.client.domain.usecase.LoginWithCodeUseCase
import com.v2rayops.client.domain.usecase.LoginWithDeepLinkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginWithCodeUseCase: LoginWithCodeUseCase,
    private val loginWithDeepLinkUseCase: LoginWithDeepLinkUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun loginWithPayload(payload: AuthDeepLinkPayload) {
        val clean = payload.customerCode.trim()
        if (clean.isBlank()) {
            _uiState.value = AuthUiState.Error("Authorization link contains an invalid customer code")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            AppLogger.i("AuthViewModel", "Processing authorization link for customer: $clean")
            when (val result = loginWithDeepLinkUseCase(payload)) {
                is AppResult.Success -> {
                    _uiState.value = AuthUiState.Success(result.data.customerCode)
                }
                is AppResult.Failure -> {
                    _uiState.value = AuthUiState.Error(result.error.message)
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun login(customerCode: String) {
        val clean = customerCode.trim()
        if (clean.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your V2RayOPS customer code")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = loginWithCodeUseCase(clean)) {
                is AppResult.Success -> {
                    _uiState.value = AuthUiState.Success(result.data.customerCode)
                }
                is AppResult.Failure -> {
                    _uiState.value = AuthUiState.Error(result.error.message)
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    /**
     * Launches the web authentication flow in Chrome Custom Tabs with Cyber Ops dark obsidian theme.
     * Uses shared device cookies so logged-in web users authorize automatically.
     */
    fun openWebAuthorization(context: Context) {
        val baseUrl = BuildConfig.DEFAULT_BASE_URL.trimEnd('/')
        val webUrl = "$baseUrl/auth/mobile-callback"
        AppLogger.i("AuthViewModel", "Launching web authorization portal: $webUrl")

        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(android.graphics.Color.parseColor("#050811"))
                        .setNavigationBarColor(android.graphics.Color.parseColor("#050811"))
                        .build()
                )
                .build()

            customTabsIntent.launchUrl(context, Uri.parse(webUrl))
        } catch (e: Exception) {
            AppLogger.w("AuthViewModel", "Custom Tabs failed, falling back to standard browser: ${e.message}")
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                context.startActivity(intent)
            } catch (err: Exception) {
                _uiState.value = AuthUiState.Error("Could not open web browser: ${err.message}")
            }
        }
    }

    fun loginDevOfflineMode() {
        _uiState.value = AuthUiState.Success("267968")
    }

    fun resetError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }
}
