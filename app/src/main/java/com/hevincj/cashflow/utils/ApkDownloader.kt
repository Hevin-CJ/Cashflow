package com.hevincj.cashflow.utils

import android.content.Context
import android.util.Log
import com.hevincj.cashflow.domain.models.AppUpdateInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DownloadStatus {
    object Idle : DownloadStatus
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val isPatch: Boolean = false
    ) : DownloadStatus
    data class Patching(val message: String = "Applying update patch...") : DownloadStatus
    data class Completed(val apkFile: File) : DownloadStatus
    data class Error(val message: String) : DownloadStatus
}

@Singleton
class ApkDownloader @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder().build()

    companion object {
        private const val TAG = "ApkDownloader"
    }

    /**
     * Downloads and prepares the APK for installation.
     * If a binary delta patch is available, it attempts to download and apply the patch.
     * If delta patching is unavailable or fails at any stage, it automatically falls back
     * to downloading the complete standalone APK.
     */
    fun downloadUpdate(updateInfo: AppUpdateInfo): Flow<DownloadStatus> = flow {
        emit(DownloadStatus.Downloading(0f, 0L, 0L, isPatch = updateInfo.isDeltaPatch))

        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        updatesDir.listFiles()?.forEach { it.delete() }

        val destinationApk = File(updatesDir, "CashFlow-v${updateInfo.latestVersion}.apk")

        // 1. Attempt Binary Delta Patch if available
        if (updateInfo.patchDownloadUrl != null) {
            val patchFile = File(updatesDir, "delta.patch")
            var patchDownloadSuccess = false

            try {
                com.hevincj.cashflow.utils.CrashLogger.d(TAG, "Attempting delta patch update from: ${updateInfo.patchDownloadUrl}")
                val downloaded = downloadToFile(
                    url = updateInfo.patchDownloadUrl,
                    destination = patchFile,
                    isPatch = true,
                    onProgress = { progress, downloadedBytes, totalBytes ->
                        emit(DownloadStatus.Downloading(progress, downloadedBytes, totalBytes, isPatch = true))
                    }
                )
                patchDownloadSuccess = downloaded
            } catch (e: Exception) {
                com.hevincj.cashflow.utils.CrashLogger.w(TAG, "Failed to download delta patch: ${e.message}. Falling back to full APK.", e)
            }

            if (patchDownloadSuccess && patchFile.exists() && patchFile.length() > 0) {
                emit(DownloadStatus.Patching())

                try {
                    val currentApk = File(context.applicationInfo.sourceDir)
                    if (currentApk.exists()) {
                        com.hevincj.cashflow.utils.CrashLogger.d(TAG, "Applying BSDIFF40 patch against installed APK (${currentApk.length()} bytes)...")
                        val patchSuccess = BsPatch.applyPatch(
                            oldFile = currentApk,
                            patchFile = patchFile,
                            newFile = destinationApk
                        )

                        if (patchSuccess && destinationApk.exists() && destinationApk.length() > 0) {
                            com.hevincj.cashflow.utils.CrashLogger.i(TAG, "Delta patch applied successfully! Output APK: ${destinationApk.length()} bytes")
                            patchFile.delete()
                            emit(DownloadStatus.Completed(destinationApk))
                            return@flow
                        } else {
                            com.hevincj.cashflow.utils.CrashLogger.w(TAG, "BsPatch synthesis failed. Falling back to full APK download.")
                        }
                    } else {
                        com.hevincj.cashflow.utils.CrashLogger.w(TAG, "Source APK not accessible at ${currentApk.path}. Falling back.")
                    }
                } catch (e: Exception) {
                    com.hevincj.cashflow.utils.CrashLogger.w(TAG, "Error applying delta patch: ${e.message}. Falling back to full APK.", e)
                } finally {
                    if (patchFile.exists()) patchFile.delete()
                }
            }

            // Cleanup partial synthesized APK if patch failed
            if (destinationApk.exists()) {
                destinationApk.delete()
            }
        }

        // 2. Full APK Download (Direct or Fallback)
        com.hevincj.cashflow.utils.CrashLogger.d(TAG, "Downloading full APK from: ${updateInfo.downloadUrl}")
        emit(DownloadStatus.Downloading(0f, 0L, 0L, isPatch = false))

        try {
            val fullDownloadSuccess = downloadToFile(
                url = updateInfo.downloadUrl,
                destination = destinationApk,
                isPatch = false,
                onProgress = { progress, downloadedBytes, totalBytes ->
                    emit(DownloadStatus.Downloading(progress, downloadedBytes, totalBytes, isPatch = false))
                }
            )

            if (fullDownloadSuccess && destinationApk.exists() && destinationApk.length() > 0) {
                emit(DownloadStatus.Completed(destinationApk))
            } else {
                emit(DownloadStatus.Error("Failed to download complete APK package."))
            }
        } catch (e: Exception) {
            emit(DownloadStatus.Error(e.localizedMessage ?: "Unknown download error"))
        }
    }.flowOn(Dispatchers.IO)

    fun downloadApk(url: String, versionName: String): Flow<DownloadStatus> = flow {
        val dummyInfo = AppUpdateInfo(
            isUpdateAvailable = true,
            latestVersion = versionName,
            currentVersion = "",
            releaseTitle = "",
            releaseNotes = "",
            downloadUrl = url,
            apkSize = 0L
        )
        downloadUpdate(dummyInfo).collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    private suspend fun downloadToFile(
        url: String,
        destination: File,
        isPatch: Boolean,
        onProgress: suspend (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Boolean {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IllegalStateException("HTTP ${response.code}: ${response.message}")
        }

        val body = response.body ?: throw IllegalStateException("Empty response body from server")
        val totalBytes = body.contentLength()
        var downloadedBytes = 0L

        body.byteStream().use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var lastEmittedProgress = 0f

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val progress = if (totalBytes > 0) {
                        (downloadedBytes.toFloat() / totalBytes.toFloat()) * 100f
                    } else {
                        0f
                    }

                    if (progress - lastEmittedProgress >= 1f || downloadedBytes == totalBytes) {
                        lastEmittedProgress = progress
                        onProgress(progress, downloadedBytes, totalBytes)
                    }
                }
                output.flush()
            }
        }
        return true
    }
}
