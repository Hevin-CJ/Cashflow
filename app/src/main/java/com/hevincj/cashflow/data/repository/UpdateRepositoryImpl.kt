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

            val cleanCurrent = currentVersionName.trim().removePrefix("v").removePrefix("V")
            val cleanRemote = release.tagName.trim().removePrefix("v").removePrefix("V")

            val patchAsset = release.assets.firstOrNull { asset ->
                val name = asset.name.lowercase()
                name.endsWith(".patch") && (
                    name.contains("patch-v${cleanCurrent}-to-v${cleanRemote}") ||
                    name.contains("patch-${cleanCurrent}-to-${cleanRemote}") ||
                    name.contains("patch-v${cleanCurrent}-to-${cleanRemote}") ||
                    name.contains("patch-${cleanCurrent}-to-v${cleanRemote}")
                )
            }

            val isNewer = isNewerVersion(remoteTag = release.tagName, currentVersion = currentVersionName)
            val updateInfo = AppUpdateInfo(
                isUpdateAvailable = isNewer && apkAsset != null,
                latestVersion = cleanRemote,
                currentVersion = cleanCurrent,
                releaseTitle = release.name ?: release.tagName,
                releaseNotes = formatReleaseNotes(release.body),
                downloadUrl = apkAsset?.browserDownloadUrl ?: "",
                apkSize = apkAsset?.size ?: 0L,
                patchDownloadUrl = patchAsset?.browserDownloadUrl,
                patchSize = patchAsset?.size
            )
            Result.success(updateInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatReleaseNotes(rawBody: String?): String {
        if (rawBody.isNullOrBlank()) return "• Bug fixes and performance improvements."

        val cleaned = rawBody
            .lines()
            .filterNot { it.contains("Full Changelog", ignoreCase = true) }
            .filterNot { it.trim().startsWith("https://github.com/") }
            .map { line ->
                line.replace(Regex("\\s+by\\s+@[\\w-]+(\\s+in\\s+https://\\S+)?"), "")
                    .replace(Regex("in https://\\S+"), "")
                    .replace(Regex("^#+\\s*"), "")
                    .trim()
            }
            .filter { line ->
                line.isNotEmpty() && !line.equals("What's Changed", ignoreCase = true)
            }
            .joinToString("\n") { line ->
                val trimmed = line.trimStart('*', '-', '•', ' ')
                "• $trimmed"
            }
            .trim()

        return if (cleaned.isBlank()) {
            "• Bug fixes and performance improvements."
        } else {
            cleaned
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
