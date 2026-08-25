package com.hevincj.cashflow.utils

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class FileOpenerTest {

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var packageManager: PackageManager

    @Mock
    lateinit var uri: Uri

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(uri.normalizeScheme()).thenReturn(uri)
        whenever(context.packageManager).thenReturn(packageManager)
        whenever(context.packageName).thenReturn("com.hevincj.cashflow")
    }

    @Test
    fun testCsvCandidateMimeTypesPrioritization() {
        val candidates = FileOpener.getCandidateMimeTypes("text/csv")
        assertTrue(candidates.contains("text/csv"))
        assertTrue(candidates.contains("text/comma-separated-values"))
        assertTrue(candidates.contains("application/vnd.ms-excel"))
        assertTrue(candidates.contains("application/csv"))
        assertTrue(candidates.contains("application/x-csv"))
        assertTrue(candidates.contains("text/plain"))
        assertTrue(candidates.contains("text/*"))
        assertTrue(candidates.contains("*/*"))
        assertEquals("text/csv", candidates.first())
    }

    @Test
    fun testPdfCandidateMimeTypesPrioritization() {
        val candidates = FileOpener.getCandidateMimeTypes("application/pdf")
        assertEquals(listOf("application/pdf", "*/*"), candidates)
    }

    @Test
    fun testCustomMimeTypesPrioritization() {
        val candidates = FileOpener.getCandidateMimeTypes("image/png")
        assertEquals(listOf("image/png", "*/*"), candidates)
    }

    @Test
    fun testOpenFileExecutesFallbackGracefullyWhenNoViewersFound() {
        whenever(packageManager.queryIntentActivities(any(), any<Int>()))
            .thenReturn(emptyList())

        FileOpener.openFile(context, uri, "text/csv")
        verify(context).startActivity(any())
    }
}
