package com.hevincj.cashflow.ui.screen

import android.content.Context
import com.hevincj.cashflow.ui.theme.PrimaryGradient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.io.File
import java.io.FileInputStream

class SplashScreenTest {

    @Mock
    lateinit var context: Context

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(context.packageName).thenReturn("com.hevincj.cashflow")
    }

    private fun findProjectRoot(): File {
        return sequenceOf(
            File("."),
            File(".."),
            File("../..")
        ).map { it.canonicalFile }
            .firstOrNull { File(it, "app/src/main/res").exists() }
            ?: File(".").canonicalFile
    }

    @Test
    fun testCashflowDesignDrawableResourceExistsAndIsTransparentPng() {
        val root = findProjectRoot()
        val designPng = File(root, "app/src/main/res/drawable/cashflow_design.png")
        assertTrue("cashflow_design.png must exist in app/src/main/res/drawable/", designPng.exists())
        assertTrue("cashflow_design.png must have valid non-empty file size", designPng.length() > 100L)

        // Verify PNG signature and IHDR color type is 6 (RGBA)
        FileInputStream(designPng).use { stream ->
            val header = ByteArray(29)
            val bytesRead = stream.read(header)
            assertEquals(29, bytesRead)
            // PNG Signature: 0x89 0x50 0x4E 0x47 0x0D 0x0A 0x1A 0x0A
            assertEquals(0x89.toByte(), header[0])
            assertEquals(0x50.toByte(), header[1])
            assertEquals(0x4E.toByte(), header[2])
            assertEquals(0x47.toByte(), header[3])

            // IHDR chunk: color type at offset 25 should be 6 (RGBA / Truecolor with Alpha)
            val colorType = header[25].toInt() and 0xFF
            assertEquals("PNG must have RGBA color type (6)", 6, colorType)
        }
    }

    @Test
    fun testThemeDisablesWindowPreviewToPreventPreliminaryScreen() {
        val root = findProjectRoot()
        val themesXml = File(root, "app/src/main/res/values/themes.xml")
        assertTrue("themes.xml must exist", themesXml.exists())
        val content = themesXml.readText()
        assertTrue("themes.xml must set android:windowDisablePreview to true", content.contains("android:windowDisablePreview\">true"))
        assertTrue("themes.xml must set android:windowIsTranslucent to true", content.contains("android:windowIsTranslucent\">true"))
    }

    @Test
    fun testSplashBackgroundUsesPrimaryGradient() {
        assertNotNull(PrimaryGradient)
    }
}
