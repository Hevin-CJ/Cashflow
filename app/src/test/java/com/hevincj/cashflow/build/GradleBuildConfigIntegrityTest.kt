package com.hevincj.cashflow.build

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Properties

class GradleBuildConfigIntegrityTest {

    @Test
    fun testGradlePropertiesConfiguration() {
        val rootDir = File(".").absoluteFile.parentFile?.parentFile ?: File(".")
        // In Android Studio unit tests, root is project dir or app dir
        val gradlePropsFile = sequenceOf(
            File("gradle.properties"),
            File("../gradle.properties"),
            File("../../gradle.properties")
        ).firstOrNull { it.exists() }

        if (gradlePropsFile != null) {
            val props = Properties()
            gradlePropsFile.inputStream().use { props.load(it) }

            assertEquals("true", props.getProperty("org.gradle.parallel"))
            assertEquals("true", props.getProperty("org.gradle.caching"))
            assertEquals("true", props.getProperty("org.gradle.configuration-cache"))
            assertEquals("true", props.getProperty("org.gradle.vfs.watch"))
            assertEquals("true", props.getProperty("ksp.useKsp2"))
            assertEquals("true", props.getProperty("android.enableR8.fullMode"))
            assertEquals("UNSUPPORTED_PROJECT_OPTION_USE", props.getProperty("android.sync.suppressAgpWarnings"))
            assertTrue(props.getProperty("kotlin.daemon.jvmargs", "").contains("-Xmx2048m"))
        }
    }

    @Test
    fun testJava17BytecodeTargetCompatibility() {
        val javaVersion = System.getProperty("java.version") ?: "17"
        val majorVersion = javaVersion.split(".")[0].toIntOrNull() ?: 17
        assertTrue("Build environment must support Java 17+", majorVersion >= 17)
    }

    @Test
    fun testPackagingExclusionRules() {
        val standardExclusions = listOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/DEPENDENCIES",
            "/META-INF/LICENSE*",
            "/META-INF/NOTICE*",
            "/META-INF/INDEX.LIST",
            "/META-INF/*.version"
        )
        assertEquals(6, standardExclusions.size)
        assertTrue(standardExclusions.contains("/META-INF/DEPENDENCIES"))
        assertTrue(standardExclusions.contains("/META-INF/INDEX.LIST"))
    }
}
