package com.hevincj.cashflow.build

import android.content.Context
import com.hevincj.cashflow.utils.ApkInstaller
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.io.File

class ApkSigningIntegrityTest {

    @Mock
    lateinit var mockContext: Context

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(mockContext.packageName).thenReturn("com.hevincj.cashflow")
    }

    private fun findProjectRoot(): File {
        return sequenceOf(
            File("."),
            File(".."),
            File("../..")
        ).map { it.canonicalFile }
            .firstOrNull { File(it, "app/build.gradle.kts").exists() }
            ?: File(".").canonicalFile
    }

    @Test
    fun testBuildGradleConfiguresAllSigningSchemes() {
        val root = findProjectRoot()
        val buildGradle = File(root, "app/build.gradle.kts")
        assertTrue("app/build.gradle.kts must exist", buildGradle.exists())

        val content = buildGradle.readText()
        assertTrue("Must enable V1 signing", content.contains("enableV1Signing = true"))
        assertTrue("Must enable V2 signing", content.contains("enableV2Signing = true"))
        assertTrue("Must enable V3 signing", content.contains("enableV3Signing = true"))
        assertTrue("Must enable V4 signing", content.contains("enableV4Signing = true"))
    }

    @Test
    fun testReleaseBuildUsesReleaseSigningConfig() {
        val root = findProjectRoot()
        val buildGradle = File(root, "app/build.gradle.kts")
        val content = buildGradle.readText()

        assertTrue("Release buildType must use release signingConfig", content.contains("signingConfig = signingConfigs.getByName(\"release\")"))
    }

    @Test
    fun testApkInstallerFailsGracefullyOnNonExistentApk() {
        val nonExistentFile = File("/tmp/non_existent_update_${System.currentTimeMillis()}.apk")
        val result = ApkInstaller.installApk(mockContext, nonExistentFile)

        assertTrue("Installation should return failure Result for missing file", result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun testApkInstallerSessionParamsContract() {
        val params = ApkInstaller.createSessionParams("com.hevincj.cashflow")
        assertNotNull("SessionParams must be created", params)
    }
}
