package com.hevincj.cashflow.data.remote.api

import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SignatureInterceptor : Interceptor {
    
    // Obfuscated string: "7c6a9926-e17f-4318-875f-28b939f5ef74"
    // XOR-obfuscated with key 0x5A
    private val apiSecret: String
        get() {
            val obfuscated = byteArrayOf(
                109, 57, 108, 59, 99, 99, 104, 108, 119, 63, 107, 109, 60, 119, 110, 105, 107, 98, 119, 98, 109, 111, 60, 119, 104, 98, 56, 99, 105, 99, 60, 111, 63, 60, 109, 110
            )
            val key = 0x5A
            val bytes = ByteArray(obfuscated.size)
            for (i in obfuscated.indices) {
                bytes[i] = (obfuscated[i].toInt() xor key).toByte()
            }
            return String(bytes, Charsets.UTF_8)
        }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val timestamp = System.currentTimeMillis().toString()
        val nonce = UUID.randomUUID().toString()
        val method = originalRequest.method
        val path = originalRequest.url.encodedPath

        val dataToSign = "$method|$path|$timestamp|$nonce"
        val signature = hmacSha256(apiSecret, dataToSign)

        val newRequest = originalRequest.newBuilder()
            .header("X-App-Signature", signature)
            .header("X-App-Timestamp", timestamp)
            .header("X-App-Nonce", nonce)
            .build()

        return chain.proceed(newRequest)
    }

    private fun hmacSha256(key: String, data: String): String {
        val sha256HMAC = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
        sha256HMAC.init(secretKey)
        val hashBytes = sha256HMAC.doFinal(data.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
