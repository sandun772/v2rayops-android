package com.v2rayops.client.core.update

import android.content.Context
import com.v2rayops.client.core.logger.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class DownloadProgress {
    data object Starting : DownloadProgress()
    data class Progress(
        val bytesRead: Long,
        val totalBytes: Long,
        val percentage: Int,
        val speedMbps: Double
    ) : DownloadProgress()
    data class Completed(val apkFile: File) : DownloadProgress()
    data class Error(val error: Throwable) : DownloadProgress()
}

@Singleton
class AppUpdateDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "AppUpdateDownloader"
        private const val BUFFER_SIZE = 8192
    }

    private val downloadClient = okHttpClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Downloads an APK from [downloadUrl] and streams progress updates.
     * The downloaded file is placed in [context.cacheDir]/updates/[fileName].
     */
    fun downloadApk(downloadUrl: String, fileName: String = "v2rayops-update.apk"): Flow<DownloadProgress> = flow {
        emit(DownloadProgress.Starting)
        AppLogger.i(TAG, "Starting download from: $downloadUrl")

        val updatesDir = File(context.cacheDir, "updates")
        if (!updatesDir.exists()) {
            updatesDir.mkdirs()
        }

        val targetFile = File(updatesDir, fileName)
        if (targetFile.exists()) {
            targetFile.delete()
        }

        val tempFile = File(updatesDir, "$fileName.tmp")
        if (tempFile.exists()) {
            tempFile.delete()
        }

        val request = Request.Builder()
            .url(downloadUrl)
            .header("User-Agent", "V2RayOps-Android-Updater")
            .build()

        try {
            val response = downloadClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorMsg = "HTTP download failed with code: ${response.code}"
                AppLogger.e(TAG, errorMsg)
                emit(DownloadProgress.Error(IllegalStateException(errorMsg)))
                return@flow
            }

            val body = response.body
            if (body == null) {
                val errorMsg = "Response body is empty"
                AppLogger.e(TAG, errorMsg)
                emit(DownloadProgress.Error(IllegalStateException(errorMsg)))
                return@flow
            }

            val totalBytes = body.contentLength()
            var bytesReadTotal = 0L
            val startTime = System.currentTimeMillis()
            var lastSampleTime = startTime
            var lastSampleBytes = 0L
            var currentSpeedMbps = 0.0

            body.byteStream().use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        bytesReadTotal += bytesRead

                        val now = System.currentTimeMillis()
                        val sampleDelta = now - lastSampleTime

                        // Recalculate speed every 250ms
                        if (sampleDelta >= 250) {
                            val bytesDelta = bytesReadTotal - lastSampleBytes
                            val seconds = sampleDelta / 1000.0
                            if (seconds > 0) {
                                // Megabytes per second
                                currentSpeedMbps = (bytesDelta / (1024.0 * 1024.0)) / seconds
                            }
                            lastSampleTime = now
                            lastSampleBytes = bytesReadTotal
                        }

                        val percent = if (totalBytes > 0) {
                            ((bytesReadTotal * 100) / totalBytes).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }

                        emit(
                            DownloadProgress.Progress(
                                bytesRead = bytesReadTotal,
                                totalBytes = totalBytes,
                                percentage = percent,
                                speedMbps = currentSpeedMbps
                            )
                        )
                    }
                    outputStream.flush()
                }
            }

            if (tempFile.renameTo(targetFile)) {
                AppLogger.i(TAG, "Download finished successfully: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
                emit(DownloadProgress.Completed(targetFile))
            } else {
                val errorMsg = "Failed to rename temp file to target apk"
                AppLogger.e(TAG, errorMsg)
                emit(DownloadProgress.Error(IllegalStateException(errorMsg)))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error downloading APK: ${e.message}", e)
            if (tempFile.exists()) {
                tempFile.delete()
            }
            emit(DownloadProgress.Error(e))
        }
    }.flowOn(Dispatchers.IO)
}
