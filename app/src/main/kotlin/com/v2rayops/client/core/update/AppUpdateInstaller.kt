package com.v2rayops.client.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.v2rayops.client.core.logger.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class InstallResult {
    data object Success : InstallResult()
    data object PermissionRequired : InstallResult()
    data class Failure(val reason: String) : InstallResult()
}

@Singleton
class AppUpdateInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AppUpdateInstaller"
    }

    /**
     * Checks if the app has permission to install unknown apps (Android 8.0+).
     */
    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Launches the system settings screen to allow installing unknown packages for this app.
     */
    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to open unknown app install settings", e)
            }
        }
    }

    /**
     * Initiates APK installation via Android Package Installer.
     */
    fun installApk(apkFile: File): InstallResult {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            val error = "Target APK file does not exist or is empty: ${apkFile.absolutePath}"
            AppLogger.e(TAG, error)
            return InstallResult.Failure(error)
        }

        if (!canInstallPackages()) {
            AppLogger.w(TAG, "REQUEST_INSTALL_PACKAGES permission not granted yet by user.")
            openInstallPermissionSettings()
            return InstallResult.PermissionRequired
        }

        return try {
            val authority = "${context.packageName}.provider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            AppLogger.i(TAG, "Dispatching Package Installer Intent for: $apkUri")
            context.startActivity(installIntent)
            InstallResult.Success
        } catch (e: Exception) {
            val error = "Failed to launch package installer: ${e.message}"
            AppLogger.e(TAG, error, e)
            InstallResult.Failure(error)
        }
    }
}
