package com.hevincj.cashflow.data.repository

import com.hevincj.cashflow.domain.models.ReceiptErrorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ReceiptScanErrorMappingTest {

    private fun mapHttpError(code: Int, errorBody: String): Pair<String, ReceiptErrorType> {
        return when {
            code == 400 -> "Receipt image is blurry or difficult to read. Please take a clearer photo." to ReceiptErrorType.UNREADABLE_IMAGE
            code == 429 -> "AI service is busy with high traffic. Please retry in a few moments." to ReceiptErrorType.RATE_LIMITED
            code == 500 && errorBody.contains("GEMINI_API_KEY", ignoreCase = true) -> "Gemini API key is not configured on the backend server." to ReceiptErrorType.CONFIG_ERROR
            code in 500..599 -> "Server error ($code). Receipt analysis service temporarily unavailable." to ReceiptErrorType.SERVER_ERROR
            else -> "Failed to analyze receipt (HTTP $code)." to ReceiptErrorType.UNKNOWN
        }
    }

    private fun mapException(e: Throwable): Pair<String, ReceiptErrorType> {
        return when (e) {
            is UnknownHostException -> "No internet connection. Please check your network and retry." to ReceiptErrorType.NETWORK_ERROR
            is SocketTimeoutException -> "Connection timed out. Please try with a clearer or smaller photo." to ReceiptErrorType.NETWORK_ERROR
            is ConnectException -> "Unable to connect to receipt backend server." to ReceiptErrorType.NETWORK_ERROR
            else -> (e.localizedMessage ?: "Unexpected error during receipt analysis.") to ReceiptErrorType.UNKNOWN
        }
    }

    @Test
    fun testHttp400MapsToUnreadableImage() {
        val (msg, type) = mapHttpError(400, "Bad Request")
        assertEquals(ReceiptErrorType.UNREADABLE_IMAGE, type)
        assertTrue(msg.contains("blurry", ignoreCase = true))
    }

    @Test
    fun testHttp429MapsToRateLimited() {
        val (msg, type) = mapHttpError(429, "Too Many Requests")
        assertEquals(ReceiptErrorType.RATE_LIMITED, type)
        assertTrue(msg.contains("busy", ignoreCase = true))
    }

    @Test
    fun testHttp500WithApiKeyMapsToConfigError() {
        val (msg, type) = mapHttpError(500, "Verify GEMINI_API_KEY is configured")
        assertEquals(ReceiptErrorType.CONFIG_ERROR, type)
        assertTrue(msg.contains("API key", ignoreCase = true))
    }

    @Test
    fun testHttp500GeneralMapsToServerError() {
        val (msg, type) = mapHttpError(500, "Internal Server Error")
        assertEquals(ReceiptErrorType.SERVER_ERROR, type)
        assertTrue(msg.contains("Server error", ignoreCase = true))
    }

    @Test
    fun testUnknownHostExceptionMapsToNetworkError() {
        val (msg, type) = mapException(UnknownHostException("Unable to resolve host"))
        assertEquals(ReceiptErrorType.NETWORK_ERROR, type)
        assertTrue(msg.contains("internet", ignoreCase = true))
    }

    @Test
    fun testSocketTimeoutExceptionMapsToNetworkError() {
        val (msg, type) = mapException(SocketTimeoutException("timeout"))
        assertEquals(ReceiptErrorType.NETWORK_ERROR, type)
        assertTrue(msg.contains("timed out", ignoreCase = true))
    }

    @Test
    fun testConnectExceptionMapsToNetworkError() {
        val (msg, type) = mapException(ConnectException("Connection refused"))
        assertEquals(ReceiptErrorType.NETWORK_ERROR, type)
        assertTrue(msg.contains("connect", ignoreCase = true))
    }
}
