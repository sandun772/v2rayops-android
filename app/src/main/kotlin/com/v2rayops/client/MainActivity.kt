package com.v2rayops.client

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.core.update.AppUpdateManager
import com.v2rayops.client.core.update.LocalAppUpdateManager
import com.v2rayops.client.core.update.UpdateUiState
import com.v2rayops.client.designsystem.theme.V2RayOpsTheme
import com.v2rayops.client.domain.repository.AuthDeepLinkPayload
import com.v2rayops.client.network.iphunt.IpHuntManager
import com.v2rayops.client.ui.navigation.AppNavGraph
import com.v2rayops.client.ui.update.AppUpdateDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var ipHuntManager: IpHuntManager

    @Inject
    lateinit var appUpdateManager: AppUpdateManager

    private val deepLinkPayload = mutableStateOf<AuthDeepLinkPayload?>(null)

    override fun onResume() {
        super.onResume()
        ipHuntManager.refreshAutomationCapabilities()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_V2RayOps)
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            CompositionLocalProvider(
                LocalAppUpdateManager provides appUpdateManager
            ) {
                V2RayOpsTheme {
                    val navController = rememberNavController()
                    val updateState by appUpdateManager.updateState.collectAsState()

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF050811)
                    ) {
                        AppNavGraph(
                            navController = navController,
                            deepLinkPayload = deepLinkPayload.value,
                            onDeepLinkHandled = { deepLinkPayload.value = null }
                        )
                    }

                    // Global OTA Update Dialog
                    AppUpdateDialog(
                        state = updateState,
                        onDownloadClick = {
                            val state = updateState
                            if (state is UpdateUiState.UpdateAvailable) {
                                appUpdateManager.startDownload(state.updateInfo)
                            }
                        },
                        onInstallClick = {
                            val state = updateState
                            if (state is UpdateUiState.ReadyToInstall) {
                                appUpdateManager.installApk(state.apkFile)
                            }
                        },
                        onDismiss = {
                            appUpdateManager.dismiss()
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        AppLogger.i("MainActivity", "Received deep link uri: $uri")

        // Extracts customer code & auth parameters from:
        // - v2rayops://auth/callback?code=...&token=...&email=...&name=...
        // - https://v2rayops.vpnxhub.com/auth/mobile-callback?code=...
        val code = uri.getQueryParameter("code") ?: uri.getQueryParameter("customer_code")
        val token = uri.getQueryParameter("token") ?: uri.getQueryParameter("auth_token")
        val email = uri.getQueryParameter("email")
        val name = uri.getQueryParameter("name")

        if (!code.isNullOrBlank()) {
            AppLogger.i("MainActivity", "Extracted authorization payload from deep link for customer: $code")
            deepLinkPayload.value = AuthDeepLinkPayload(
                customerCode = code,
                token = token,
                email = email,
                name = name
            )
        }
    }
}
