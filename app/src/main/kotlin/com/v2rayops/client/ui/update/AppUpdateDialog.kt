package com.v2rayops.client.ui.update

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.v2rayops.client.core.update.UpdateUiState
import java.util.Locale

@Composable
fun AppUpdateDialog(
    state: UpdateUiState,
    onDownloadClick: () -> Unit,
    onInstallClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state is UpdateUiState.Idle) return

    // Only show dialog for UpdateAvailable, Downloading, ReadyToInstall, or Error (if manual)
    val shouldShowDialog = state is UpdateUiState.UpdateAvailable ||
            state is UpdateUiState.Downloading ||
            state is UpdateUiState.ReadyToInstall ||
            (state is UpdateUiState.Error && state.isManual)

    if (!shouldShowDialog) return

    Dialog(
        onDismissRequest = {
            if (state !is UpdateUiState.Downloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = state !is UpdateUiState.Downloading,
            dismissOnClickOutside = state !is UpdateUiState.Downloading,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp)),
            color = Color(0xFF0C101B),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header Row: Status badge and Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Glowing Status Capsule
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF0F172A), RoundedCornerShape(20.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dotColor = when (state) {
                            is UpdateUiState.UpdateAvailable -> Color(0xFF06B6D4)
                            is UpdateUiState.Downloading -> Color(0xFF6366F1)
                            is UpdateUiState.ReadyToInstall -> Color(0xFF10B981)
                            is UpdateUiState.Error -> Color(0xFFEF4444)
                            else -> Color(0xFF64748B)
                        }

                        val badgeLabel = when (state) {
                            is UpdateUiState.UpdateAvailable -> "NEW UPDATE AVAILABLE"
                            is UpdateUiState.Downloading -> "OTA DOWNLOAD IN PROGRESS"
                            is UpdateUiState.ReadyToInstall -> "READY FOR INSTALLATION"
                            is UpdateUiState.Error -> "UPDATE ERROR"
                            else -> "OTA SYSTEM"
                        }

                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = badgeLabel,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = dotColor,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (state !is UpdateUiState.Downloading) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content based on state
                when (state) {
                    is UpdateUiState.UpdateAvailable -> {
                        UpdateAvailableContent(
                            state = state,
                            onDownloadClick = onDownloadClick,
                            onDismiss = onDismiss
                        )
                    }

                    is UpdateUiState.Downloading -> {
                        DownloadingContent(
                            state = state,
                            onCancel = onDismiss
                        )
                    }

                    is UpdateUiState.ReadyToInstall -> {
                        ReadyToInstallContent(
                            state = state,
                            onInstallClick = onInstallClick,
                            onDismiss = onDismiss
                        )
                    }

                    is UpdateUiState.Error -> {
                        ErrorContent(
                            errorMessage = state.message,
                            onDismiss = onDismiss
                        )
                    }

                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun UpdateAvailableContent(
    state: UpdateUiState.UpdateAvailable,
    onDownloadClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val info = state.updateInfo

    Column(modifier = Modifier.fillMaxWidth()) {
        // Version Comparison
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VersionTag(label = "CURRENT", version = info.currentVersion, color = Color(0xFF94A3B8))
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF475569),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            VersionTag(label = "TARGET", version = info.latestVersion, color = Color(0xFF06B6D4))

            Spacer(modifier = Modifier.weight(1f))

            if (info.apkSizeBytes > 0) {
                Text(
                    text = formatBytes(info.apkSizeBytes),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Title
        Text(
            text = info.releaseTitle,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF8FAFC),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Release Notes Card
        Text(
            text = "CHANGELOG & IMPROVEMENTS",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = Color(0xFF818CF8)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 140.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF070B14))
                .border(1.dp, Color(0xFF161F30), RoundedCornerShape(10.dp))
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = info.changelog.ifBlank { "Maintenance release with performance optimizations and stability improvements." },
                fontSize = 12.sp,
                color = Color(0xFFCBD5E1),
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Buttons
        Button(
            onClick = onDownloadClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6366F1),
                contentColor = Color.White
            ),
            border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.6f))
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "DOWNLOAD & INSTALL",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
        ) {
            Text(
                text = "LATER",
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DownloadingContent(
    state: UpdateUiState.Downloading,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Downloading V2RayOps ${state.updateInfo.latestVersion}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF8FAFC)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Cyber Linear Progress Indicator
        LinearProgressIndicator(
            progress = { state.percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Color(0xFF6366F1),
            trackColor = Color(0xFF1E293B)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Stats row: Percentage, bytes, speed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${state.percentage}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF06B6D4)
            )

            val speedText = if (state.speedMbps > 0) {
                String.format(Locale.US, "%.1f MB/s", state.speedMbps)
            } else {
                "Calculating..."
            }

            Text(
                text = speedText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF818CF8)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            val progressText = if (state.totalBytes > 0) {
                "${formatBytes(state.bytesRead)} / ${formatBytes(state.totalBytes)}"
            } else {
                formatBytes(state.bytesRead)
            }

            Text(
                text = progressText,
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF64748B)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
        ) {
            Text(
                text = "CANCEL DOWNLOAD",
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ReadyToInstallContent(
    state: UpdateUiState.ReadyToInstall,
    onInstallClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFF062B20))
                .border(1.dp, Color(0xFF0B6A4B), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Download Complete",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF8FAFC)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Version ${state.updateInfo.latestVersion} has been verified and is ready for system package installation.",
            fontSize = 12.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onInstallClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF10B981),
                contentColor = Color.White
            ),
            border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.6f))
        ) {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "INSTALL NOW",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
        ) {
            Text(
                text = "CLOSE",
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ErrorContent(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF2D1214))
                .border(1.dp, Color(0xFF7F1D1D), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Update Check Failed",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF8FAFC)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = errorMessage,
            fontSize = 12.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1))
        ) {
            Text(
                text = "DISMISS",
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun VersionTag(label: String, version: String, color: Color) {
    Row(
        modifier = Modifier
            .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF64748B)
        )
        Text(
            text = if (version.startsWith("v", ignoreCase = true)) version else "v$version",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return String.format(Locale.US, "%.1f MB", mb)
}
