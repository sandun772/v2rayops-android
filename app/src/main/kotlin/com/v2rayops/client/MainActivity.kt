package com.v2rayops.client

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.compose.rememberNavController
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.designsystem.theme.V2RayOpsTheme
import com.v2rayops.client.ui.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val deepLinkCode = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            V2RayOpsTheme {
                val navController = rememberNavController()
                AppNavGraph(
                    navController = navController,
                    deepLinkCode = deepLinkCode.value,
                    onDeepLinkHandled = { deepLinkCode.value = null }
                )
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

        // Extracts customer code from v2rayops://auth/callback?code=...
        // or https://v2rayops.vpnxhub.com/auth/mobile-callback?code=...
        val code = uri.getQueryParameter("code")
        if (!code.isNullOrBlank()) {
            AppLogger.i("MainActivity", "Extracted authorization code from deep link")
            deepLinkCode.value = code
        }
    }
}
