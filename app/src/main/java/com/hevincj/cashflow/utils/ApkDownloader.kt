package com.hevincj.cashflow.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DownloadStatus {
    object Idle : DownloadStatus
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : DownloadStatus
    data class Completed(val apkFile: File) : DownloadStatus
    data class Error(val message: String) : DownloadStatus
}

@Singleton
class ApkDownloader @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder().build()

    fun downloadApk(url: String, versionName: String): Flow<DownloadStatus> = flow {
        emit(DownloadStatus.Downloading(0f, 0L, 0L))
        try {
            val updatesDir = File(context.cacheDir, "updates")
            if (!updatesDir.exists()) {
                updatesDir.mkdirs()
            } else {
                updatesDir.listFiles()?.forEach { it.delete() }
            }

            val destinationFile = File(updatesDir, "CashFlow-v$versionName.apk")
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                emit(DownloadStatus.Error("Failed to download APK: HTTP ${response.code}"))
                return@flow
            }

            val body = response.body
            if (body == null) {
                emit(DownloadStatus.Error("Empty response body from server"))
                return@flow
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                FileOutputStream(destinationFile).use { output ->
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

                        // Emit updates in ~1% increments to avoid UI flood
                        if (progress - lastEmittedProgress >= 1f || downloadedBytes == totalBytes) {
                            lastEmittedProgress = progress
                            emit(DownloadStatus.Downloading(progress, downloadedBytes, totalBytes))
                        }
                    }
                    output.flush()
                }
            }

            emit(DownloadStatus.Completed(destinationFile))
        } catch (e: Exception) {
            emit(DownloadStatus.Error(e.localizedMessage ?: "Unknown download error"))
        }
    }.flowOn(Dispatchers.IO)
}
