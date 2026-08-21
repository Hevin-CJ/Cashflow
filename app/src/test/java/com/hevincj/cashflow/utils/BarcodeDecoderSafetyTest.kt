package com.hevincj.cashflow.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeDecoderSafetyTest {

    private fun isValidProductBarcode(raw: String): Boolean {
        val isUrl = raw.startsWith("http://", ignoreCase = true) ||
                raw.startsWith("https://", ignoreCase = true) ||
                raw.startsWith("www.", ignoreCase = true) ||
                raw.contains("://", ignoreCase = true) ||
                raw.startsWith("upi://", ignoreCase = true)
        val isDigits = raw.length >= 12 && raw.all { it.isDigit() }
        return !isUrl && isDigits
    }

    @Test
    fun testValid12DigitBarcodePasses() {
        assertTrue(isValidProductBarcode("890123456789"))
    }

    @Test
    fun testValid13DigitEanBarcodePasses() {
        assertTrue(isValidProductBarcode("8901030383842"))
    }

    @Test
    fun testValid14DigitBarcodePasses() {
        assertTrue(isValidProductBarcode("18901030383849"))
    }

    @Test
    fun testShortBarcodeFails() {
        assertFalse(isValidProductBarcode("123456"))
        assertFalse(isValidProductBarcode("890123456"))
    }

    @Test
    fun testAlphanumericCodeFails() {
        assertFalse(isValidProductBarcode("89012345678A"))
        assertFalse(isValidProductBarcode("ABCD-1234-5678"))
    }

    @Test
    fun testHttpAndHttpsUrlsFail() {
        assertFalse(isValidProductBarcode("http://example.com/item/123456789012"))
        assertFalse(isValidProductBarcode("https://cashflow.app/p/8901030383842"))
        assertFalse(isValidProductBarcode("www.google.com"))
    }

    @Test
    fun testUpiPaymentLinksFailInBarcodeMode() {
        assertFalse(isValidProductBarcode("upi://pay?pa=merchant@upi&pn=Store&am=100"))
    }
}
