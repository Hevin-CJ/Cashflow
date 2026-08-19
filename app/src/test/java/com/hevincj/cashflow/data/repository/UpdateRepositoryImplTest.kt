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
    fun `isNewerVersion correctly compares semantic versions`() {
        assertTrue(repository.isNewerVersion("v1.0.6", "1.0.5"))
        assertTrue(repository.isNewerVersion("1.0.6", "1.0.5-debug"))
        assertTrue(repository.isNewerVersion("v2.0.0", "v1.9.9"))
        assertFalse(repository.isNewerVersion("v1.0.6", "1.0.6"))
        assertFalse(repository.isNewerVersion("v1.0.5", "1.0.6"))
    }
}
