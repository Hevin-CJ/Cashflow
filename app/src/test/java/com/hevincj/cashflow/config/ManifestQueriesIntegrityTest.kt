package com.hevincj.cashflow.config

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ManifestQueriesIntegrityTest {

    @Test
    fun testManifestContainsRequiredDocumentViewAndSendQueries() {
        val possiblePaths = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
            File("../app/src/main/AndroidManifest.xml")
        )
        val manifestFile = possiblePaths.firstOrNull { it.exists() }
        assertTrue("AndroidManifest.xml should exist", manifestFile != null && manifestFile.exists())

        val content = manifestFile!!.readText()

        // 1. Must query ACTION_VIEW for document MIME types
        assertTrue("Manifest must query ACTION_VIEW for text/csv", content.contains("<data android:mimeType=\"text/csv\" />"))
        assertTrue("Manifest must query ACTION_VIEW for text/comma-separated-values", content.contains("<data android:mimeType=\"text/comma-separated-values\" />"))
        assertTrue("Manifest must query ACTION_VIEW for text/*", content.contains("<data android:mimeType=\"text/*\" />"))
        assertTrue("Manifest must query ACTION_VIEW for application/pdf", content.contains("<data android:mimeType=\"application/pdf\" />"))
        assertTrue("Manifest must query ACTION_VIEW for */*", content.contains("<data android:mimeType=\"*/*\" />"))

        // 2. Must query ACTION_SEND for sharing/saving fallback
        assertTrue("Manifest must query ACTION_SEND for */*", content.contains("<action android:name=\"android.intent.action.SEND\" />"))
    }
}
