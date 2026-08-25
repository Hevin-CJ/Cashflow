package com.hevincj.cashflow.performance

import android.graphics.BitmapFactory
import com.hevincj.cashflow.utils.ImageSamplingUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseOptimizationAndMemoryTest {

    @Test
    fun testInSampleSizeCalculation_smallImageNeedsNoDownsampling() {
        val options = BitmapFactory.Options().apply {
            outWidth = 800
            outHeight = 600
        }
        val sampleSize = ImageSamplingUtils.calculateInSampleSize(options, 1280, 1280)
        assertEquals(1, sampleSize)
    }

    @Test
    fun testInSampleSizeCalculation_largeImageDownsampledToPowerOfTwo() {
        val options = BitmapFactory.Options().apply {
            outWidth = 4000
            outHeight = 3000
        }
        val sampleSize = ImageSamplingUtils.calculateInSampleSize(options, 1280, 1280)
        // 4000/2 = 2000 > 1280, 4000/4 = 1000 < 1280 -> sampleSize is 2
        assertEquals(2, sampleSize)
    }

    @Test
    fun testInSampleSizeCalculation_hugeImageDownsampledAggressively() {
        val options = BitmapFactory.Options().apply {
            outWidth = 8000
            outHeight = 6000
        }
        val sampleSize = ImageSamplingUtils.calculateInSampleSize(options, 1280, 1280)
        // 8000/2=4000, 8000/4=2000, 8000/8=1000 -> sampleSize is 4
        assertEquals(4, sampleSize)
    }

    @Test
    fun testEmptyByteArrayReturnsNull() {
        val result = ImageSamplingUtils.decodeSampledBitmapFromByteArray(ByteArray(0))
        assertNull(result)
    }

    @Test
    fun testPackagingExclusionsInBuildGradle() {
        val buildGradleFile = File("build.gradle.kts")
        if (buildGradleFile.exists()) {
            val content = buildGradleFile.readText()
            assertTrue(content.contains("splits"))
            assertTrue(content.contains("arm64-v8a"))
            assertTrue(content.contains("META-INF/**/LICENSE*"))
        }
    }

    @Test
    fun testProguardOptimizationRules() {
        val proguardFile = File("proguard-rules.pro")
        if (proguardFile.exists()) {
            val content = proguardFile.readText()
            assertTrue(content.contains("-optimizationpasses 5"))
            assertTrue(content.contains("-allowaccessmodification"))
            assertTrue(content.contains("-repackageclasses"))
        }
    }
}
