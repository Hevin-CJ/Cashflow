package com.hevincj.cashflow.utils

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class FileOpenerTest {

    @Test
    fun testGetCandidateMimeTypesForCsvContainsAllKnownFormats() {
        val candidates = FileOpener.getCandidateMimeTypes("text/csv")
        assertEquals(
            listOf(
                "text/csv",
                "text/comma-separated-values",
                "application/vnd.ms-excel",
                "application/csv",
                "application/x-csv",
                "text/plain",
                "text/*",
                "*/*"
            ),
            candidates
        )
        assertTrue("Primary CSV MIME must be first", candidates.first() == "text/csv")
        assertTrue("Wildcard must be last fallback", candidates.last() == "*/*")
    }

    @Test
    fun testGetCandidateMimeTypesForPdfContainsPdfAndWildcard() {
        val candidates = FileOpener.getCandidateMimeTypes("application/pdf")
        assertEquals(listOf("application/pdf", "*/*"), candidates)
    }

    @Test
    fun testGetCandidateMimeTypesForGenericType() {
        val candidates = FileOpener.getCandidateMimeTypes("image/png")
        assertEquals(listOf("image/png", "*/*"), candidates)
    }

    @Test
    fun testBuildViewIntentReturnsNonNullIntent() {
        val uri = mock<Uri>()
        val intent = FileOpener.buildViewIntent(uri, "text/csv")
        assertNotNull(intent)
    }

    @Test
    fun testBuildShareIntentReturnsNonNullIntent() {
        val uri = mock<Uri>()
        val intent = FileOpener.buildShareIntent(uri, "text/csv")
        assertNotNull(intent)
    }
}
