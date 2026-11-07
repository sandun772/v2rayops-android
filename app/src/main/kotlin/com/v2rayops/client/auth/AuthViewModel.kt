package com.v2rayops.client.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.domain.usecase.LoginWithCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginWithCodeUseCase: LoginWithCodeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

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

    fun loginDevOfflineMode() {
        _uiState.value = AuthUiState.Success("267968")
    }

    fun resetError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }
}
