package com.hevincj.cashflow.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpiUriParserTest {

    @Test
    fun testParseValidUpiUri() {
        val uri = "upi://pay?pa=john.doe@okaxis&pn=John%20Doe&am=150.50&tn=Coffee%20Share&cu=INR"
        val data = UpiUriParser.parse(uri)

        assertTrue(data.isValid)
        assertNull(data.errorMessage)
        assertEquals("john.doe@okaxis", data.payeeVpa)
        assertEquals("John Doe", data.payeeName)
        assertEquals(150.50, data.amount ?: 0.0, 0.001)
        assertEquals("Coffee Share", data.note)
        assertEquals("INR", data.currency)
    }

    @Test
    fun testParseMissingAmountAndNote() {
        val uri = "upi://pay?pa=shop@hdfcbank&pn=HDFC%20Shop"
        val data = UpiUriParser.parse(uri)

        assertTrue(data.isValid)
        assertEquals("shop@hdfcbank", data.payeeVpa)
        assertEquals("HDFC Shop", data.payeeName)
        assertNull(data.amount)
        assertNull(data.note)
        assertEquals("INR", data.currency)
    }

    @Test
    fun testParseMissingPa() {
        val uri = "upi://pay?pn=John%20Doe&am=150.50"
        val data = UpiUriParser.parse(uri)

        assertFalse(data.isValid)
        assertTrue(data.errorMessage?.contains("pa") == true)
    }

    @Test
    fun testParseInvalidVpaFormat() {
        val uri = "upi://pay?pa=invalid_vpa_no_at&pn=John%20Doe"
        val data = UpiUriParser.parse(uri)

        assertFalse(data.isValid)
        assertTrue(data.errorMessage?.contains("UPI ID format") == true)
    }

    @Test
    fun testParseMissingPn() {
        val uri = "upi://pay?pa=john@okaxis"
        val data = UpiUriParser.parse(uri)

        assertFalse(data.isValid)
        assertTrue(data.errorMessage?.contains("pn") == true)
    }

    @Test
    fun testParseInvalidAmount() {
        val uri = "upi://pay?pa=john@okaxis&pn=John&am=-20"
        val data = UpiUriParser.parse(uri)

        assertFalse(data.isValid)
        assertTrue(data.errorMessage?.contains("positive decimal") == true)
    }

    @Test
    fun testParseUnsupportedCurrency() {
        val uri = "upi://pay?pa=john@okaxis&pn=John&am=100&cu=USD"
        val data = UpiUriParser.parse(uri)

        assertFalse(data.isValid)
        assertTrue(data.errorMessage?.contains("INR") == true)
    }

    @Test
    fun testNoteTruncation() {
        val longNote = "A".repeat(100)
        val uri = "upi://pay?pa=john@okaxis&pn=John&tn=$longNote"
        val data = UpiUriParser.parse(uri)

        assertTrue(data.isValid)
        assertEquals(80, data.note?.length)
        assertEquals("A".repeat(80), data.note)
    }
}
