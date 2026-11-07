package com.v2rayops.client.auth

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val customerCode: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
}
