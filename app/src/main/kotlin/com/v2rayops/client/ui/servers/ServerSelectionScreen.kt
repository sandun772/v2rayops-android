package com.v2rayops.client.ui.servers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.v2rayops.client.designsystem.components.CyberTopAppBar
import com.v2rayops.client.designsystem.theme.CyberAccent
import com.v2rayops.client.designsystem.theme.CyberBackground
import com.v2rayops.client.designsystem.theme.CyberTextMuted
import com.v2rayops.client.designsystem.theme.CyberTextPrimary
import com.v2rayops.client.ui.servers.components.ServerListItem

@Composable
fun ServerSelectionScreen(
    onBackClick: () -> Unit,
    viewModel: ServerSelectionViewModel = hiltViewModel()
) {
    val configs by viewModel.configs.collectAsState()
    val selectedConfig by viewModel.selectedConfig.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CyberTopAppBar(
                title = "ACTIVE CONFIGURATIONS",
                onBackClick = onBackClick,
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshConfigs() },
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(18.dp),
                                strokeWidth = 2.dp,
                                color = CyberAccent
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = CyberTextPrimary
                            )
                        }
                    }
                }
            )

            if (configs.isEmpty() && isRefreshing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CyberAccent)
                }
            } else if (configs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NO ACTIVE CONFIGS FOUND",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your customer account does not have active package configurations provisioned yet. Log in or purchase a package on v2rayops.vpnxhub.com to sync your configurations.",
                            fontSize = 12.sp,
                            color = CyberTextMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    items(configs, key = { it.id }) { config ->
                        val isSelected = config.id == selectedConfig?.id
                        ServerListItem(
                            config = config,
                            isSelected = isSelected,
                            onSelect = {
                                viewModel.selectConfig(config)
                                onBackClick()
                            },
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
