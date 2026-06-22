package com.hevincj.cashflow.data.remote.api

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SignatureInterceptorTest {

    @Mock
    lateinit var chain: Interceptor.Chain

    @Mock
    lateinit var response: Response

    private lateinit var interceptor: SignatureInterceptor

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        interceptor = SignatureInterceptor()
    }

    @Test
    fun testInterceptAppendsSignatureHeaders() {
        // 1. Arrange: Create a dummy request representing a GET to /api/transactions
        val request = Request.Builder()
            .url("https://cashflow-ktor-backend-703934017156.asia-south2.run.app/api/transactions")
            .method("GET", null)
            .build()

        whenever(chain.request()).thenReturn(request)
        whenever(chain.proceed(any())).thenReturn(response)

        // 2. Act: Execute interceptor
        val result = interceptor.intercept(chain)

        // 3. Assert: Verify proceed was called, capture the modified request
        val requestCaptor = argumentCaptor<Request>()
        verify(chain).proceed(requestCaptor.capture())
        
        assertEquals(response, result)

        val interceptedRequest = requestCaptor.firstValue
        assertNotNull("Intercepted request should not be null", interceptedRequest)

        val signature = interceptedRequest.header("X-App-Signature")
        val timestamp = interceptedRequest.header("X-App-Timestamp")
        val nonce = interceptedRequest.header("X-App-Nonce")

        assertNotNull("Signature header X-App-Signature must be present", signature)
        assertNotNull("Timestamp header X-App-Timestamp must be present", timestamp)
        assertNotNull("Nonce header X-App-Nonce must be present", nonce)

        assertTrue("Signature should be a valid SHA-256 hash string", signature!!.length == 64)
        assertTrue("Timestamp should be a valid epoch millisecond string", timestamp!!.toLong() > 0)
        assertNotNull("Nonce should be a valid UUID string", java.util.UUID.fromString(nonce))

        println("Verified headers added by SignatureInterceptor:")
        println("  X-App-Signature: $signature")
        println("  X-App-Timestamp: $timestamp")
        println("  X-App-Nonce: $nonce")
    }
}
