package com.v2rayops.client.designsystem.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.core.update.LocalAppUpdateManager
import com.v2rayops.client.core.update.UpdateUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FutureReleaseCard(
    badgeText: String,
    title: String,
    subtitle: String,
    description: String,
    icon: ImageVector,
    versionTag: String = "SCHEDULED: FUTURE RELEASE",
    modifier: Modifier = Modifier,
    onCheckUpdates: (() -> Unit)? = null,
    additionalAction: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appUpdateManager = LocalAppUpdateManager.current
    val updateState: UpdateUiState = appUpdateManager?.updateState?.collectAsState()?.value ?: UpdateUiState.Idle

    var localChecking by remember { mutableStateOf(false) }
    var checkResult by remember { mutableStateOf<String?>(null) }
    var isErrorResult by remember { mutableStateOf(false) }

    val isChecking = localChecking || updateState is UpdateUiState.Checking

    LaunchedEffect(updateState) {
        when (updateState) {
            is UpdateUiState.UpToDate -> {
                if (updateState.isManual) {
                    localChecking = false
                    isErrorResult = false
                    checkResult = "SYSTEM IS UP TO DATE • CLIENT v${updateState.currentVersion}"
                    Toast.makeText(context, "V2RayOPS is up to date (v${updateState.currentVersion})", Toast.LENGTH_SHORT).show()
                }
            }
            is UpdateUiState.Error -> {
                if (updateState.isManual) {
                    localChecking = false
                    isErrorResult = true
                    checkResult = "CHECK FAILED: ${updateState.message}"
                }
            }
            is UpdateUiState.UpdateAvailable -> {
                localChecking = false
                checkResult = null
            }
            else -> Unit
        }
    }

    LaunchedEffect(checkResult) {
        if (checkResult != null) {
            delay(5000)
            checkResult = null
            isErrorResult = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0C101B))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Status Capsule Pill
            Row(
                modifier = Modifier
                    .background(Color(0xFF0F1528), RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFF1D2845), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF06B6D4))
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = badgeText,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF06B6D4)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Center Icon Hexagon / Squircle
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0E1424))
                    .border(1.dp, Color(0xFF24324F), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF818CF8),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title & Subtitle
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp,
                color = Color(0xFFF8FAFC),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF06B6D4),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Body Description
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Interactive Check Result Alert Banner
            AnimatedVisibility(
                visible = checkResult != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val bannerBg = if (isErrorResult) Color(0xFF2B1115) else Color(0xFF041C14)
                val bannerBorder = if (isErrorResult) Color(0xFF6B1822) else Color(0xFF0B4834)
                val iconTint = if (isErrorResult) Color(0xFFEF4444) else Color(0xFF10B981)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(bannerBg)
                        .border(1.dp, bannerBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isErrorResult) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = checkResult ?: "",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = iconTint
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Primary Check For Updates Button
            Button(
                onClick = {
                    if (!isChecking) {
                        checkResult = null
                        if (onCheckUpdates != null) {
                            onCheckUpdates()
                        } else if (appUpdateManager != null) {
                            appUpdateManager.checkForUpdates(isManual = true)
                        } else {
                            // Fallback simulation if no manager in preview
                            localChecking = true
                            scope.launch {
                                delay(1200)
                                localChecking = false
                                checkResult = "SYSTEM IS UP TO DATE • CLIENT v1.0.0"
                                Toast.makeText(context, "V2RayOPS is up to date (v1.0.0)", Toast.LENGTH_SHORT).show()
                                delay(4000)
                                checkResult = null
                            }
                        }
                    }
                },
                enabled = !isChecking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1),
                    disabledContainerColor = Color(0xFF6366F1).copy(alpha = 0.5f),
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.6f)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "CHECKING REPOSITORIES...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CHECK FOR UPDATES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp,
                        color = Color.White
                    )
                }
            }

            // Optional Additional Action (e.g. Sign Out)
            if (additionalAction != null) {
                Spacer(modifier = Modifier.height(12.dp))
                additionalAction()
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Target release tag
            Text(
                text = versionTag,
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF475569),
                letterSpacing = 0.8.sp
            )
        }
    }
}
