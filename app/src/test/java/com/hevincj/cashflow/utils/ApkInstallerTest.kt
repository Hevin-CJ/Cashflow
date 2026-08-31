package com.hevincj.cashflow.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.io.File

class ApkInstallerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var packageManager: PackageManager

    @Mock
    lateinit var uri: Uri

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(context.packageName).thenReturn("com.hevincj.cashflow")
        whenever(context.packageManager).thenReturn(packageManager)
    }

    @Test
    fun testApkMimeTypeConstant() {
        assertEquals("application/vnd.android.package-archive", ApkInstaller.APK_MIME_TYPE)
    }

    @Test
    fun testInstallApkFailsWhenFileDoesNotExist() {
        val nonExistentFile = File(tempFolder.root, "non_existent_update.apk")
        val result = ApkInstaller.installApk(context, nonExistentFile)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("APK file does not exist or is empty.", result.exceptionOrNull()?.message)
    }

    @Test
    fun testInstallApkFailsWhenFileIsEmpty() {
        val emptyFile = tempFolder.newFile("empty_update.apk")
        val result = ApkInstaller.installApk(context, emptyFile)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("APK file does not exist or is empty.", result.exceptionOrNull()?.message)
    }

    @Test
    fun testCreateInstallIntentConfiguration() {
        val intent = ApkInstaller.createInstallIntent(context, uri)
        assertNotNull("Intent must not be null", intent)
        assertTrue(intent is Intent)
    }

    @Test
    fun testCreatePermissionSettingsIntentConfiguration() {
        val intent = ApkInstaller.createPermissionSettingsIntent("com.hevincj.cashflow")
        assertNotNull("Permission settings intent must not be null", intent)
        assertTrue(intent is Intent)
    }

    @Test
    fun testCanRequestPackageInstallsQueriesPackageManager() {
        whenever(packageManager.canRequestPackageInstalls()).thenReturn(true)
        val canInstall = ApkInstaller.canRequestPackageInstalls(context)
        // On JVM (SDK_INT == 0), returns default fallback true
        assertTrue(canInstall)
    }
}

