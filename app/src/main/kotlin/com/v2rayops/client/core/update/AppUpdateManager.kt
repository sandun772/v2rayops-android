package com.v2rayops.client.core.update

import android.content.Context
import com.v2rayops.client.BuildConfig
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.data.api.GitHubReleaseApi
import com.v2rayops.client.domain.model.UpdateInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class UpdateUiState {
    data object Idle : UpdateUiState()
    data class Checking(val isManual: Boolean) : UpdateUiState()
    data class UpToDate(val currentVersion: String, val isManual: Boolean) : UpdateUiState()
    data class UpdateAvailable(val updateInfo: UpdateInfo) : UpdateUiState()
    data class Downloading(
        val updateInfo: UpdateInfo,
        val percentage: Int,
        val bytesRead: Long,
        val totalBytes: Long,
        val speedMbps: Double
    ) : UpdateUiState()
    data class ReadyToInstall(
        val updateInfo: UpdateInfo,
        val apkFile: File
    ) : UpdateUiState()
    data class Error(val message: String, val isManual: Boolean) : UpdateUiState()
}

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gitHubReleaseApi: GitHubReleaseApi,
    private val downloader: AppUpdateDownloader,
    private val installer: AppUpdateInstaller
) {
    companion object {
        private const val TAG = "AppUpdateManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var downloadJob: Job? = null

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    val currentVersion: String
        get() = BuildConfig.VERSION_NAME

    /**
     * Checks GitHub for new releases.
     * @param isManual True if initiated by the user tapping a button, false if triggered automatically.
     */
    fun checkForUpdates(isManual: Boolean = true) {
        if (_updateState.value is UpdateUiState.Checking || _updateState.value is UpdateUiState.Downloading) {
            return
        }

        _updateState.value = UpdateUiState.Checking(isManual)
        AppLogger.i(TAG, "Checking for updates (current: v$currentVersion, manual: $isManual)...")

        scope.launch(Dispatchers.IO) {
            try {
                val owner = BuildConfig.GITHUB_REPO_OWNER
                val repo = BuildConfig.GITHUB_REPO_NAME

                val response = try {
                    gitHubReleaseApi.getLatestRelease(owner = owner, repo = repo)
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Direct GitHub release check failed (${e.message}). Attempting fallback relay...")
                    val fallbackUrl = "${BuildConfig.DEFAULT_BASE_URL.trimEnd('/')}/api/client/update-check"
                    gitHubReleaseApi.getReleaseFromUrl(fallbackUrl)
                }

                if (response.code() == 404) {
                    // Repository has no releases published yet or repo is not yet populated
                    AppLogger.w(TAG, "No GitHub releases found for $owner/$repo (HTTP 404). Current build is up to date.")
                    _updateState.value = UpdateUiState.UpToDate(currentVersion, isManual)
                    return@launch
                }

                if (!response.isSuccessful || response.body() == null) {
                    val errorMsg = "Update check failed (HTTP ${response.code()})"
                    AppLogger.e(TAG, errorMsg)
                    _updateState.value = UpdateUiState.Error(errorMsg, isManual)
                    return@launch
                }

                val release = response.body()!!
                val remoteVersion = release.tagName
                AppLogger.i(TAG, "Latest release found: ${release.tagName} (name: ${release.name})")

                val isNewer = VersionComparator.isNewer(currentVersion, remoteVersion)
                val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }

                if (isNewer && apkAsset != null) {
                    val updateInfo = UpdateInfo(
                        currentVersion = currentVersion,
                        latestVersion = remoteVersion,
                        releaseTitle = release.name ?: "Release $remoteVersion",
                        changelog = release.body ?: "Bug fixes and performance improvements.",
                        downloadUrl = apkAsset.downloadUrl,
                        apkSizeBytes = apkAsset.size,
                        hasUpdate = true,
                        releaseDate = release.publishedAt
                    )
                    AppLogger.i(TAG, "New update available: $remoteVersion (APK: ${apkAsset.name}, ${apkAsset.size} bytes)")
                    _updateState.value = UpdateUiState.UpdateAvailable(updateInfo)
                } else {
                    AppLogger.i(TAG, "Client is up to date (current: v$currentVersion, latest: $remoteVersion)")
                    _updateState.value = UpdateUiState.UpToDate(currentVersion, isManual)
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error checking for updates: ${e.message}", e)
                _updateState.value = UpdateUiState.Error(
                    message = e.localizedMessage ?: "Network connection error",
                    isManual = isManual
                )
            }
        }
    }

    /**
     * Starts downloading the APK update.
     */
    fun startDownload(updateInfo: UpdateInfo) {
        downloadJob?.cancel()
        _updateState.value = UpdateUiState.Downloading(
            updateInfo = updateInfo,
            percentage = 0,
            bytesRead = 0,
            totalBytes = updateInfo.apkSizeBytes,
            speedMbps = 0.0
        )

        downloadJob = scope.launch(Dispatchers.IO) {
            val fileName = "v2rayops-${VersionComparator.normalizeVersion(updateInfo.latestVersion)}.apk"
            downloader.downloadApk(updateInfo.downloadUrl, fileName).collect { progress ->
                when (progress) {
                    is DownloadProgress.Starting -> {
                        _updateState.value = UpdateUiState.Downloading(
                            updateInfo = updateInfo,
                            percentage = 0,
                            bytesRead = 0,
                            totalBytes = updateInfo.apkSizeBytes,
                            speedMbps = 0.0
                        )
                    }
                    is DownloadProgress.Progress -> {
                        _updateState.value = UpdateUiState.Downloading(
                            updateInfo = updateInfo,
                            percentage = progress.percentage,
                            bytesRead = progress.bytesRead,
                            totalBytes = if (progress.totalBytes > 0) progress.totalBytes else updateInfo.apkSizeBytes,
                            speedMbps = progress.speedMbps
                        )
                    }
                    is DownloadProgress.Completed -> {
                        AppLogger.i(TAG, "Download finished. Ready to install: ${progress.apkFile.absolutePath}")
                        _updateState.value = UpdateUiState.ReadyToInstall(
                            updateInfo = updateInfo,
                            apkFile = progress.apkFile
                        )
                        // Trigger installation
                        installer.installApk(progress.apkFile)
                    }
                    is DownloadProgress.Error -> {
                        val errorMsg = progress.error.message ?: "Failed to download update"
                        AppLogger.e(TAG, "Download error: $errorMsg", progress.error)
                        _updateState.value = UpdateUiState.Error(errorMsg, isManual = true)
                    }
                }
            }
        }
    }

    /**
     * Re-triggers APK installation from cached file.
     */
    fun installApk(apkFile: File) {
        installer.installApk(apkFile)
    }

    /**
     * Dismisses the update dialog / resets state to Idle.
     */
    fun dismiss() {
        if (_updateState.value is UpdateUiState.Downloading) {
            downloadJob?.cancel()
        }
        _updateState.value = UpdateUiState.Idle
    }

    /**
     * Simulation method for testing update UI flows without requiring a live GitHub release asset.
     */
    fun simulateUpdate(
        targetVersion: String = "1.0.1",
        title: String = "V2RayOps Cyber Update",
        changelog: String = "• Next-gen IP Hunt with automated Airplane Mode cycling\n• Enhanced VLESS tunneling stability\n• Added live bandwidth monitoring in HUD",
        mockUrl: String = "https://v2rayops.vpnxhub.com:2083/downloads/v2rayops-client-debug.apk",
        mockSize: Long = 45 * 1024 * 1024L
    ) {
        _updateState.value = UpdateUiState.UpdateAvailable(
            UpdateInfo(
                currentVersion = currentVersion,
                latestVersion = targetVersion,
                releaseTitle = title,
                changelog = changelog,
                downloadUrl = mockUrl,
                apkSizeBytes = mockSize,
                hasUpdate = true
            )
        )
    }
}
