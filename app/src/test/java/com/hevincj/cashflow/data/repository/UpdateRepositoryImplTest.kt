package com.hevincj.cashflow.data.repository

import com.hevincj.cashflow.data.remote.api.GithubApi
import com.hevincj.cashflow.data.remote.models.GithubReleaseAssetDto
import com.hevincj.cashflow.data.remote.models.GithubReleaseDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class UpdateRepositoryImplTest {

    private lateinit var githubApi: GithubApi
    private lateinit var repository: UpdateRepositoryImpl

    @Before
    fun setUp() {
        githubApi = mock(GithubApi::class.java)
        repository = UpdateRepositoryImpl(githubApi)
    }

    @Test
    fun `formatReleaseNotes filters out generic version release lines and preserves real changes`() {
        val rawBody = """
            ## What's Changed
            * 1.0.6 release
            * Release v1.0.6
            * Bump version to 1.0.6
            * Added Package Comparison For Newer Versions
            * Fixed Profile Screen Crash on Logout
            
            **Full Changelog**: https://github.com/Hevin-CJ/Cashflow/compare/v1.0.5...v1.0.6
        """.trimIndent()

        val formatted = repository.formatReleaseNotes(rawBody)

        assertFalse(formatted.contains("1.0.6 release", ignoreCase = true))
        assertFalse(formatted.contains("Release v1.0.6", ignoreCase = true))
        assertFalse(formatted.contains("Bump version", ignoreCase = true))
        assertFalse(formatted.contains("Full Changelog", ignoreCase = true))
        assertTrue(formatted.contains("• Added Package Comparison For Newer Versions"))
        assertTrue(formatted.contains("• Fixed Profile Screen Crash on Logout"))
    }

    @Test
    fun `formatReleaseNotes deduplicates repeated release note lines case-insensitively`() {
        val rawBody = """
            ## What's Changed
            * Fixed recurring subscription repeating issue
            * fixed recurring subscription repeating issue
            * Fixed Barcode Scanner Crashing Issue
            * Fixed Recurring Subscription Repeating Issue
            * Fixed Profile Screen Crash on Logout
        """.trimIndent()

        val formatted = repository.formatReleaseNotes(rawBody)
        val lines = formatted.lines()

        assertEquals(3, lines.size)
        assertEquals("• Fixed recurring subscription repeating issue", lines[0])
        assertEquals("• Fixed Barcode Scanner Crashing Issue", lines[1])
        assertEquals("• Fixed Profile Screen Crash on Logout", lines[2])
    }

    @Test
    fun `checkForUpdate discovers patch asset with semantic regex matching`() = runTest {
        val releaseDto = GithubReleaseDto(
            tagName = "v1.0.6",
            name = "CashFlow v1.0.6",
            body = "* Added Package Comparison For Newer Versions",
            publishedAt = "2026-08-18T13:03:32Z",
            htmlUrl = "https://github.com/Hevin-CJ/Cashflow/releases/tag/v1.0.6",
            assets = listOf(
                GithubReleaseAssetDto(
                    name = "CashFlow-v1.0.6.apk",
                    size = 18500000L,
                    browserDownloadUrl = "https://github.com/download/CashFlow-v1.0.6.apk",
                    contentType = "application/vnd.android.package-archive"
                ),
                GithubReleaseAssetDto(
                    name = "patch-v1.0.5-to-v1.0.6.patch",
                    size = 1100000L,
                    browserDownloadUrl = "https://github.com/download/patch-v1.0.5-to-v1.0.6.patch",
                    contentType = "application/octet-stream"
                )
            )
        )

        `when`(githubApi.getLatestRelease()).thenReturn(releaseDto)

        val result = repository.checkForUpdate(currentVersionName = "1.0.5-debug")
        assertTrue(result.isSuccess)

        val updateInfo = result.getOrNull()
        assertNotNull(updateInfo)
        assertTrue(updateInfo!!.isUpdateAvailable)
        assertTrue(updateInfo.isDeltaPatch)
        assertEquals("1.0.6", updateInfo.latestVersion)
        assertEquals("1.0.5", updateInfo.currentVersion)
        assertEquals(1100000L, updateInfo.patchSize)
        assertEquals("https://github.com/download/patch-v1.0.5-to-v1.0.6.patch", updateInfo.patchDownloadUrl)
    }

    @Test
    fun `checkForUpdate falls back gracefully to full APK when patch asset is not present`() = runTest {
        val releaseDto = GithubReleaseDto(
            tagName = "v1.0.6",
            name = "CashFlow v1.0.6",
            body = "* Added new features",
            publishedAt = "2026-08-18T13:03:32Z",
            htmlUrl = "https://github.com/Hevin-CJ/Cashflow/releases/tag/v1.0.6",
            assets = listOf(
                GithubReleaseAssetDto(
                    name = "CashFlow-v1.0.6.apk",
                    size = 18500000L,
                    browserDownloadUrl = "https://github.com/download/CashFlow-v1.0.6.apk",
                    contentType = "application/vnd.android.package-archive"
                )
            )
        )

        `when`(githubApi.getLatestRelease()).thenReturn(releaseDto)

        val result = repository.checkForUpdate(currentVersionName = "1.0.2")
        assertTrue(result.isSuccess)

        val updateInfo = result.getOrNull()
        assertNotNull(updateInfo)
        assertTrue(updateInfo!!.isUpdateAvailable)
        assertFalse(updateInfo.isDeltaPatch)
        assertNull(updateInfo.patchDownloadUrl)
        assertEquals(18500000L, updateInfo.apkSize)
    }

    @Test
    fun `formatMultiReleaseNotes aggregates multiple versions with version headers`() {
        val releases = listOf(
            GithubReleaseDto(
                tagName = "v1.1.0",
                name = "CashFlow v1.1.0",
                body = "* Added new analytics dashboard\n* General performance improvements",
                publishedAt = "2026-08-20T00:00:00Z",
                htmlUrl = "https://github.com/Hevin-CJ/Cashflow/releases/tag/v1.1.0",
                assets = emptyList()
            ),
            GithubReleaseDto(
                tagName = "v1.0.9",
                name = "CashFlow v1.0.9",
                body = "* Added export to PDF & CSV support",
                publishedAt = "2026-08-19T00:00:00Z",
                htmlUrl = "https://github.com/Hevin-CJ/Cashflow/releases/tag/v1.0.9",
                assets = emptyList()
            ),
            GithubReleaseDto(
                tagName = "v1.0.8",
                name = "CashFlow v1.0.8",
                body = "* Fixed recurring subscriptions repeating issue\n* Fixed Barcode Scanner crashing issue",
                publishedAt = "2026-08-18T00:00:00Z",
                htmlUrl = "https://github.com/Hevin-CJ/Cashflow/releases/tag/v1.0.8",
                assets = emptyList()
            )
        )

        val formatted = repository.formatMultiReleaseNotes(releases, fallbackSingleBody = null)

        assertTrue(formatted.contains("v1.1.0:\n• Added new analytics dashboard\n• General performance improvements"))
        assertTrue(formatted.contains("v1.0.9:\n• Added export to PDF & CSV support"))
        assertTrue(formatted.contains("v1.0.8:\n• Fixed recurring subscriptions repeating issue\n• Fixed Barcode Scanner crashing issue"))
    }

    @Test
    fun `checkForUpdate aggregates changelogs from all newer releases across multi-version jump`() = runTest {
        val releases = listOf(
            GithubReleaseDto(
                tagName = "v1.1.0",
                name = "CashFlow v1.1.0",
                body = "* Added new analytics dashboard",
                publishedAt = "2026-08-20T00:00:00Z",
                htmlUrl = "https://github.com/Hevin-CJ/Cashflow/releases/tag/v1.1.0",
                assets = listOf(
                    GithubReleaseAssetDto(
                        name = "CashFlow-v1.1.0.apk",
                        size = 18500000L,
                        browserDownloadUrl = "https://github.com/download/CashFlow-v1.1.0.apk",
                        contentType = "application/vnd.android.package-archive"
                    ),
                    GithubReleaseAssetDto(
                        name = "patch-v1.0.7-to-v1.1.0.patch",
                        size = 2100000L,
                        browserDownloadUrl = "https://github.com/download/patch-v1.0.7-to-v1.1.0.patch",
                        contentType = "application/octet-stream"
                    )
                )
            ),
            GithubReleaseDto(
                tagName = "v1.0.9",
                name = "CashFlow v1.0.9",
                body = "* Added export to PDF support",
                publishedAt = "2026-08-19T00:00:00Z",
                htmlUrl = "https://github.com/Hevin-CJ/Cashflow/releases/tag/v1.0.9",
                assets = emptyList()
            ),
            GithubReleaseDto(
                tagName = "v1.0.8",
                name = "CashFlow v1.0.8",
                body = "* Fixed recurring subscriptions repeating issue",
                publishedAt = "2026-08-18T00:00:00Z",
                htmlUrl = "https://github.com/Hevin-CJ/Cashflow/releases/tag/v1.0.8",
                assets = emptyList()
            ),
            GithubReleaseDto(
                tagName = "v1.0.7",
                name = "CashFlow v1.0.7",
                body = "* Initial 1.0.7 release",
                publishedAt = "2026-08-17T00:00:00Z",
                htmlUrl = "https://github.com/Hevin-CJ/Cashflow/releases/tag/v1.0.7",
                assets = emptyList()
            )
        )

        `when`(githubApi.getAllReleases()).thenReturn(releases)

        val result = repository.checkForUpdate(currentVersionName = "1.0.7")
        assertTrue(result.isSuccess)

        val updateInfo = result.getOrNull()
        assertNotNull(updateInfo)
        assertTrue(updateInfo!!.isUpdateAvailable)
        assertEquals("1.1.0", updateInfo.latestVersion)
        assertEquals("1.0.7", updateInfo.currentVersion)
        assertTrue(updateInfo.isDeltaPatch)
        assertEquals(2100000L, updateInfo.patchSize)

        // Verify that notes contain all intermediate releases 1.1.0, 1.0.9, 1.0.8 and exclude 1.0.7
        assertTrue(updateInfo.releaseNotes.contains("v1.1.0:"))
        assertTrue(updateInfo.releaseNotes.contains("• Added new analytics dashboard"))
        assertTrue(updateInfo.releaseNotes.contains("v1.0.9:"))
        assertTrue(updateInfo.releaseNotes.contains("• Added export to PDF support"))
        assertTrue(updateInfo.releaseNotes.contains("v1.0.8:"))
        assertTrue(updateInfo.releaseNotes.contains("• Fixed recurring subscriptions repeating issue"))
        assertFalse(updateInfo.releaseNotes.contains("v1.0.7:"))
    }

    @Test
    fun `isNewerVersion correctly compares semantic versions`() {
        assertTrue(repository.isNewerVersion("v1.0.6", "1.0.5"))
        assertTrue(repository.isNewerVersion("1.0.6", "1.0.5-debug"))
        assertTrue(repository.isNewerVersion("v2.0.0", "v1.9.9"))
        assertFalse(repository.isNewerVersion("v1.0.6", "1.0.6"))
        assertFalse(repository.isNewerVersion("v1.0.5", "1.0.6"))
    }
}
