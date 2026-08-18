package com.hevincj.cashflow.data.repository

import com.hevincj.cashflow.data.remote.api.GithubApi
import com.hevincj.cashflow.domain.models.AppUpdateInfo
import com.hevincj.cashflow.domain.repository.UpdateRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepositoryImpl @Inject constructor(
    private val githubApi: GithubApi
) : UpdateRepository {

    override suspend fun checkForUpdate(currentVersionName: String): Result<AppUpdateInfo> {
        return try {
            val release = githubApi.getLatestRelease()
            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                ?: release.assets.firstOrNull()

            val isNewer = isNewerVersion(remoteTag = release.tagName, currentVersion = currentVersionName)
            val updateInfo = AppUpdateInfo(
                isUpdateAvailable = isNewer && apkAsset != null,
                latestVersion = release.tagName.removePrefix("v").removePrefix("V"),
                currentVersion = currentVersionName,
                releaseTitle = release.name ?: release.tagName,
                releaseNotes = release.body ?: "No release notes provided.",
                downloadUrl = apkAsset?.browserDownloadUrl ?: "",
                apkSize = apkAsset?.size ?: 0L
            )
            Result.success(updateInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V")
        val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")

        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLength) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
