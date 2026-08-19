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

    private val semanticVersionRegex = Regex("""(\d+\.\d+(\.\d+)?)""")
    private val versionLinePattern = Regex(
        """^\s*(v?\d+\.\d+(\.\d+)?\s*(release)?|release\s*v?\d+\.\d+(\.\d+)?|bump\s+version.*|prepare\s+release.*)$""",
        RegexOption.IGNORE_CASE
    )

    override suspend fun checkForUpdate(currentVersionName: String): Result<AppUpdateInfo> {
        return try {
            val release = githubApi.getLatestRelease()
            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                ?: release.assets.firstOrNull()

            val cleanCurrent = semanticVersionRegex.find(currentVersionName)?.value
                ?: currentVersionName.trim().removePrefix("v").removePrefix("V")
            val cleanRemote = semanticVersionRegex.find(release.tagName)?.value
                ?: release.tagName.trim().removePrefix("v").removePrefix("V")

            val patchPattern = Regex(
                """^patch-v?${Regex.escape(cleanCurrent)}-to-v?${Regex.escape(cleanRemote)}\.(patch|hdiff)$""",
                RegexOption.IGNORE_CASE
            )
            val patchAsset = release.assets.firstOrNull { patchPattern.matches(it.name.trim()) }

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

    internal fun formatReleaseNotes(rawBody: String?): String {
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
                val trimmed = line.trimStart('*', '-', '•', ' ').trim()
                trimmed.isNotEmpty() &&
                !trimmed.equals("What's Changed", ignoreCase = true) &&
                !versionLinePattern.matches(trimmed)
            }
            .joinToString("\n") { line ->
                val trimmed = line.trimStart('*', '-', '•', ' ').trim()
                "• $trimmed"
            }
            .trim()

        return if (cleaned.isBlank()) {
            "• Bug fixes and performance improvements."
        } else {
            cleaned
        }
    }

    internal fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val cleanRemote = semanticVersionRegex.find(remoteTag)?.value
            ?: remoteTag.trim().removePrefix("v").removePrefix("V")
        val cleanCurrent = semanticVersionRegex.find(currentVersion)?.value
            ?: currentVersion.trim().removePrefix("v").removePrefix("V")

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
