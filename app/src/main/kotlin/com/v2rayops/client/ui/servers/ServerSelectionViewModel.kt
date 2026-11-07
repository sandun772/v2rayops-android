package com.v2rayops.client.ui.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2rayops.client.domain.model.ServerNode
import com.v2rayops.client.domain.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerSelectionViewModel @Inject constructor(
    private val serverRepository: ServerRepository
) : ViewModel() {

    val servers: StateFlow<List<ServerNode>> = serverRepository.observeServers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedServer: StateFlow<ServerNode?> = serverRepository.observeSelectedServer()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun selectServer(server: ServerNode) {
        viewModelScope.launch {
            serverRepository.selectServer(server.id)
        }
    }

    fun refreshServers() {
        viewModelScope.launch {
            _isRefreshing.value = true
            serverRepository.refreshServers()
            _isRefreshing.value = false
        }
    }
}
