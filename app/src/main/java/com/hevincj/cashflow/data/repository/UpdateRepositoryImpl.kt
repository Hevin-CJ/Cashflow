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
            val releases = try {
                val list = githubApi.getAllReleases()
                if (list.isNotEmpty()) list else listOf(githubApi.getLatestRelease())
            } catch (e: Exception) {
                listOf(githubApi.getLatestRelease())
            }

            val latestRelease = releases.firstOrNull()
                ?: return Result.failure(IllegalStateException("No releases available"))

            val apkAsset = latestRelease.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                ?: latestRelease.assets.firstOrNull()

            val cleanCurrent = semanticVersionRegex.find(currentVersionName)?.value
                ?: currentVersionName.trim().removePrefix("v").removePrefix("V")
            val cleanRemote = semanticVersionRegex.find(latestRelease.tagName)?.value
                ?: latestRelease.tagName.trim().removePrefix("v").removePrefix("V")

            val patchPattern = Regex(
                """^patch-v?${Regex.escape(cleanCurrent)}-to-v?${Regex.escape(cleanRemote)}\.(patch|hdiff)$""",
                RegexOption.IGNORE_CASE
            )
            val patchAsset = latestRelease.assets.firstOrNull { patchPattern.matches(it.name.trim()) }

            val isNewer = isNewerVersion(remoteTag = latestRelease.tagName, currentVersion = currentVersionName)
            val newerReleases = releases.filter { isNewerVersion(remoteTag = it.tagName, currentVersion = currentVersionName) }
            val aggregatedNotes = formatMultiReleaseNotes(newerReleases, fallbackSingleBody = latestRelease.body)

            val updateInfo = AppUpdateInfo(
                isUpdateAvailable = isNewer && apkAsset != null,
                latestVersion = cleanRemote,
                currentVersion = cleanCurrent,
                releaseTitle = latestRelease.name ?: latestRelease.tagName,
                releaseNotes = aggregatedNotes,
                downloadUrl = apkAsset?.browserDownloadUrl ?: "",
                apkSize = apkAsset?.size ?: 0L,
                patchDownloadUrl = patchAsset?.browserDownloadUrl,
                patchSize = patchAsset?.size
            )
            Result.success(updateInfo)
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("UpdateRepository", "Check for update failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    internal fun formatMultiReleaseNotes(
        newerReleases: List<com.hevincj.cashflow.data.remote.models.GithubReleaseDto>,
        fallbackSingleBody: String?
    ): String {
        if (newerReleases.isEmpty()) {
            return formatReleaseNotes(fallbackSingleBody)
        }
        if (newerReleases.size == 1) {
            return formatReleaseNotes(newerReleases[0].body ?: fallbackSingleBody)
        }

        val sections = newerReleases.mapNotNull { release ->
            val cleanVersion = semanticVersionRegex.find(release.tagName)?.value
                ?: release.tagName.trim().removePrefix("v").removePrefix("V")
            val formatted = formatReleaseNotes(release.body)
            if (formatted.isNotBlank()) {
                "v$cleanVersion:\n$formatted"
            } else null
        }

        return if (sections.isEmpty()) {
            formatReleaseNotes(fallbackSingleBody)
        } else {
            sections.joinToString("\n\n")
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
            .map { it.trimStart('*', '-', '•', ' ').trim() }
            .filter { trimmed ->
                trimmed.isNotEmpty() &&
                !trimmed.equals("What's Changed", ignoreCase = true) &&
                !versionLinePattern.matches(trimmed)
            }
            .distinctBy { it.lowercase(java.util.Locale.ROOT) }
            .joinToString("\n") { trimmed ->
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
