package com.hevincj.cashflow.build

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseWorkflowIntegrityTest {

    private fun findProjectRoot(): File {
        return sequenceOf(
            File("."),
            File(".."),
            File("../..")
        ).map { it.canonicalFile }
            .firstOrNull { File(it, ".github/workflows/release.yml").exists() }
            ?: File(".").canonicalFile
    }

    private fun getReleaseWorkflowFile(): File {
        val root = findProjectRoot()
        return File(root, ".github/workflows/release.yml")
    }

    private fun getAppBuildGradleFile(): File {
        val root = findProjectRoot()
        return File(root, "app/build.gradle.kts")
    }

    @Test
    fun testReleaseWorkflowTriggersAndPermissions() {
        val workflowFile = getReleaseWorkflowFile()
        assertTrue("release.yml must exist", workflowFile.exists())

        val content = workflowFile.readText()
        assertTrue("Workflow must trigger on v* tags", content.contains("tags:") && content.contains("'v*'"))
        assertTrue("Workflow must allow workflow_dispatch", content.contains("workflow_dispatch:"))
        assertTrue("Workflow must have contents: write permission", content.contains("contents: write"))
        assertTrue("Workflow must assemble release", content.contains("./gradlew assembleRelease"))
    }

    @Test
    fun testApkDiscoveryScriptContainsAllCandidateFallbacks() {
        val workflowFile = getReleaseWorkflowFile()
        assertTrue("release.yml must exist", workflowFile.exists())

        val content = workflowFile.readText()

        // Verify universal APK (from ABI splits) is prioritized
        assertTrue(
            "Workflow must check app-universal-release.apk",
            content.contains("app/build/outputs/apk/release/app-universal-release.apk")
        )

        // Verify fallback to standard app-release.apk
        assertTrue(
            "Workflow must check app-release.apk fallback",
            content.contains("app/build/outputs/apk/release/app-release.apk")
        )

        // Verify check for arm64-v8a split APK
        assertTrue(
            "Workflow must check app-arm64-v8a-release.apk",
            content.contains("app/build/outputs/apk/release/app-arm64-v8a-release.apk")
        )

        // Verify fallback wildcard search
        assertTrue(
            "Workflow must fallback to wildcard release APK search",
            content.contains("*release*.apk")
        )

        // Verify arm64 standalone artifact copy
        assertTrue(
            "Workflow must copy standalone arm64-v8a APK",
            content.contains("CashFlow-\${TAG_NAME}-arm64-v8a.apk")
        )
    }

    @Test
    fun testGradleAbiSplitsConfigured() {
        val buildGradleFile = getAppBuildGradleFile()
        assertTrue("app/build.gradle.kts must exist", buildGradleFile.exists())

        val content = buildGradleFile.readText()
        assertTrue("Splits must be enabled", content.contains("isEnable = true"))
        assertTrue("arm64-v8a must be included in splits", content.contains("\"arm64-v8a\""))
        assertTrue("isUniversalApk must be true", content.contains("isUniversalApk = true"))
    }

    @Test
    fun testReleasePublishingArtifactPatterns() {
        val workflowFile = getReleaseWorkflowFile()
        val content = workflowFile.readText()

        assertTrue(
            "Release files must include CashFlow-*.apk pattern",
            content.contains("CashFlow-*.apk")
        )
        assertTrue(
            "Release files must include patches/*.patch pattern",
            content.contains("patches/*.patch")
        )
    }

    @Test
    fun testSimulatedApkDiscoveryResolutionHierarchy() {
        // Pure Kotlin simulation of the workflow resolution logic
        fun resolveApk(availableFiles: Set<String>, tag: String): Pair<String, String?> {
            val primary = when {
                availableFiles.contains("app/build/outputs/apk/release/app-universal-release.apk") ->
                    "CashFlow-$tag.apk (from universal)"
                availableFiles.contains("app/build/outputs/apk/release/app-release.apk") ->
                    "CashFlow-$tag.apk (from legacy release)"
                availableFiles.contains("app/build/outputs/apk/release/app-arm64-v8a-release.apk") ->
                    "CashFlow-$tag.apk (from arm64)"
                else -> {
                    val fallback = availableFiles.firstOrNull { it.contains("release") && it.endsWith(".apk") }
                    if (fallback != null) "CashFlow-$tag.apk (from $fallback)" else "not_found"
                }
            }

            val arm64 = if (availableFiles.contains("app/build/outputs/apk/release/app-arm64-v8a-release.apk")) {
                "CashFlow-$tag-arm64-v8a.apk"
            } else null

            return Pair(primary, arm64)
        }

        // Scenario 1: Universal + Splits present (Current build state)
        val allSplits = setOf(
            "app/build/outputs/apk/release/app-universal-release.apk",
            "app/build/outputs/apk/release/app-arm64-v8a-release.apk",
            "app/build/outputs/apk/release/app-x86_64-release.apk"
        )
        val (p1, a1) = resolveApk(allSplits, "v1.2.0")
        assertEquals("CashFlow-v1.2.0.apk (from universal)", p1)
        assertEquals("CashFlow-v1.2.0-arm64-v8a.apk", a1)

        // Scenario 2: Legacy single APK present
        val legacy = setOf("app/build/outputs/apk/release/app-release.apk")
        val (p2, a2) = resolveApk(legacy, "v1.2.0")
        assertEquals("CashFlow-v1.2.0.apk (from legacy release)", p2)
        assertEquals(null, a2)

        // Scenario 3: Only arm64 split present
        val armOnly = setOf("app/build/outputs/apk/release/app-arm64-v8a-release.apk")
        val (p3, a3) = resolveApk(armOnly, "v1.2.0")
        assertEquals("CashFlow-v1.2.0.apk (from arm64)", p3)
        assertEquals("CashFlow-v1.2.0-arm64-v8a.apk", a3)
    }
}
