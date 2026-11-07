package com.v2rayops.client.auth.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.v2rayops.client.R
import com.v2rayops.client.auth.AuthUiState
import com.v2rayops.client.auth.AuthViewModel
import com.v2rayops.client.designsystem.components.CyberButton
import com.v2rayops.client.designsystem.components.CyberCard
import com.v2rayops.client.designsystem.components.CyberTextField
import com.v2rayops.client.designsystem.theme.CyberAccent
import com.v2rayops.client.designsystem.theme.CyberBackground
import com.v2rayops.client.designsystem.theme.CyberBorder
import com.v2rayops.client.designsystem.theme.CyberCard
import com.v2rayops.client.designsystem.theme.CyberPrimary
import com.v2rayops.client.designsystem.theme.CyberSurface
import com.v2rayops.client.designsystem.theme.CyberTextMuted
import com.v2rayops.client.designsystem.theme.CyberTextPrimary
import com.v2rayops.client.designsystem.theme.CyberTextSecondary

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    deepLinkCode: String? = null,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var customerCode by remember { mutableStateOf(deepLinkCode ?: "") }
    val context = LocalContext.current

    // Auto-trigger login when deep link code is received
    LaunchedEffect(deepLinkCode) {
        if (!deepLinkCode.isNullOrBlank()) {
            customerCode = deepLinkCode
            viewModel.login(deepLinkCode)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onLoginSuccess()
        }
    }

    val isLoading = uiState is AuthUiState.Loading
    val errorMessage = (uiState as? AuthUiState.Error)?.message

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Official Brand Logo
            Image(
                painter = painterResource(id = R.drawable.v2rayops_logo),
                contentDescription = "V2RayOPS Official Brand Logo",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "ACCESS AUTHORIZATION",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.sp,
                color = CyberTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Authorize this client via your customer code, or sign in through the web portal to link your device automatically.",
                fontSize = 13.sp,
                color = CyberTextSecondary,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Customer Code Input
            CyberTextField(
                value = customerCode,
                onValueChange = {
                    customerCode = it
                    viewModel.resetError()
                },
                label = "Customer Code",
                placeholder = "e.g. V2R-XXXXXX",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Key",
                        tint = CyberAccent,
                        modifier = Modifier.size(20.dp)
                    )
                },
                isError = errorMessage != null,
                errorMessage = errorMessage,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Button: Authorize with entered code
            CyberButton(
                onClick = { viewModel.login(customerCode) },
                isLoading = isLoading,
                enabled = customerCode.isNotBlank() && !isLoading
            ) {
                Text(
                    text = "AUTHORIZE DEVICE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Or Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(CyberBorder)
                )
                Text(
                    text = "OR SIGN IN WITH ACCOUNT",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextMuted,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(CyberBorder)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Web Sign In / Registration Button (opens website OAuth flow)
            CyberButton(
                onClick = {
                    val webUrl = "https://v2rayops.vpnxhub.com:2083/sign-in?from=/auth/mobile-callback"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                    context.startActivity(intent)
                },
                variant = com.v2rayops.client.designsystem.components.CyberButtonVariant.OUTLINED,
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = "Browser",
                    modifier = Modifier.size(18.dp),
                    tint = CyberAccent
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "SIGN IN VIA WEBSITE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp,
                    color = CyberTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dev & Offline Direct Access
            CyberButton(
                onClick = { viewModel.loginDevOfflineMode() },
                variant = com.v2rayops.client.designsystem.components.CyberButtonVariant.SECONDARY,
                enabled = !isLoading
            ) {
                Text(
                    text = "CONTINUE IN DEV / OFFLINE MODE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberAccent
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Help info card
            CyberCard(
                backgroundColor = CyberCard.copy(alpha = 0.5f),
                borderColor = CyberBorder,
                padding = 14.dp
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = CyberPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Your customer code is available in your account portal at v2rayops.vpnxhub.com. After logging in on the web, you'll be automatically redirected back into the app.",
                        fontSize = 12.sp,
                        color = CyberTextMuted,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
