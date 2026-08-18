package com.hevincj.cashflow.utils

import com.hevincj.cashflow.domain.models.AppUpdateInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BsPatchTest {

    @Test
    fun testAppUpdateInfoDeltaDetection() {
        val infoWithPatch = AppUpdateInfo(
            isUpdateAvailable = true,
            latestVersion = "1.0.5",
            currentVersion = "1.0.4",
            releaseTitle = "Release v1.0.5",
            releaseNotes = "• Bug fixes",
            downloadUrl = "https://github.com/org/repo/releases/download/v1.0.5/CashFlow-v1.0.5.apk",
            apkSize = 18000000L,
            patchDownloadUrl = "https://github.com/org/repo/releases/download/v1.0.5/patch-v1.0.4-to-v1.0.5.patch",
            patchSize = 1200000L
        )

        assertTrue(infoWithPatch.isDeltaPatch)
        assertEquals("1.0.5", infoWithPatch.latestVersion)
        assertEquals(1200000L, infoWithPatch.patchSize)

        val infoWithoutPatch = AppUpdateInfo(
            isUpdateAvailable = true,
            latestVersion = "1.0.5",
            currentVersion = "1.0.1",
            releaseTitle = "Release v1.0.5",
            releaseNotes = "• Bug fixes",
            downloadUrl = "https://github.com/org/repo/releases/download/v1.0.5/CashFlow-v1.0.5.apk",
            apkSize = 18000000L,
            patchDownloadUrl = null,
            patchSize = null
        )

        assertFalse(infoWithoutPatch.isDeltaPatch)
    }

    @Test
    fun testBsPatchHandlesMissingFilesGracefully() {
        val nonExistentOld = File("non_existent_old.apk")
        val nonExistentPatch = File("non_existent.patch")
        val output = File("output.apk")

        val result = BsPatch.applyPatch(nonExistentOld, nonExistentPatch, output)
        assertFalse(result)
        assertFalse(output.exists())
    }

    @Test
    fun testBsPatchRejectsInvalidMagicHeader() {
        val tempOld = File.createTempFile("test_old", ".apk").apply {
            writeBytes("Original APK Content".toByteArray())
            deleteOnExit()
        }
        val tempPatch = File.createTempFile("test_patch", ".patch").apply {
            writeBytes("INVALID_HEADER_DATA_1234567890".toByteArray())
            deleteOnExit()
        }
        val tempOutput = File.createTempFile("test_out", ".apk").apply {
            deleteOnExit()
        }

        val result = BsPatch.applyPatch(tempOld, tempPatch, tempOutput)
        assertFalse(result)
    }
}
