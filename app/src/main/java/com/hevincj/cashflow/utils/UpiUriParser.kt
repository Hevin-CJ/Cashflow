package com.hevincj.cashflow.utils

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class UpiPaymentData(
    val payeeVpa: String,
    val payeeName: String,
    val amount: Double?,
    val note: String?,
    val currency: String = "INR",
    val isValid: Boolean = true,
    val errorMessage: String? = null
)

object UpiUriParser {

    private val VPA_REGEX = Regex("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$")

    fun parse(uriString: String?): UpiPaymentData {
        if (uriString.isNullOrBlank()) {
            return UpiPaymentData("", "", null, null, isValid = false, errorMessage = "Empty UPI link")
        }

        if (!uriString.startsWith("upi://pay?", ignoreCase = true)) {
            return UpiPaymentData("", "", null, null, isValid = false, errorMessage = "Invalid payment scheme. Must start with upi://pay?")
        }

        try {
            val queryString = uriString.substring("upi://pay?".length)
            val params = mutableMapOf<String, String>()
            if (queryString.isNotBlank()) {
                val pairs = queryString.split("&")
                for (pair in pairs) {
                    val idx = pair.indexOf("=")
                    if (idx != -1) {
                        val key = pair.substring(0, idx)
                        val value = pair.substring(idx + 1)
                        params[key.lowercase()] = value
                    }
                }
            }

            val pa = params["pa"]?.let { decode(it) }
            val pn = params["pn"]?.let { decode(it) }
            val amStr = params["am"]?.let { decode(it) }
            val tn = params["tn"]?.let { decode(it) }
            val cu = params["cu"]?.let { decode(it) } ?: "INR"

            if (pa.isNullOrBlank()) {
                return UpiPaymentData("", "", null, null, isValid = false, errorMessage = "Missing recipient UPI ID (pa)")
            }

            if (!VPA_REGEX.matches(pa)) {
                return UpiPaymentData(pa, pn ?: "", null, null, isValid = false, errorMessage = "Invalid recipient UPI ID format")
            }

            if (pn.isNullOrBlank()) {
                return UpiPaymentData(pa, "", null, null, isValid = false, errorMessage = "Missing payee name (pn)")
            }

            val amount = if (!amStr.isNullOrBlank()) {
                val parsedAmount = amStr.toDoubleOrNull()
                if (parsedAmount == null || parsedAmount <= 0.0) {
                    return UpiPaymentData(pa, pn, null, null, isValid = false, errorMessage = "Amount must be a positive decimal number")
                }
                parsedAmount
            } else {
                null
            }

            if (cu.uppercase() != "INR") {
                return UpiPaymentData(pa, pn, amount, tn, cu, isValid = false, errorMessage = "Unsupported currency. Only INR is supported per NPCI guidelines")
            }

            val truncatedNote = tn?.take(80)

            return UpiPaymentData(
                payeeVpa = pa,
                payeeName = pn,
                amount = amount,
                note = truncatedNote,
                currency = cu.uppercase(),
                isValid = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return UpiPaymentData("", "", null, null, isValid = false, errorMessage = "Failed to parse link: ${e.message}")
        }
    }

    private fun decode(value: String): String {
        return try {
            URLDecoder.decode(value, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) {
            value
        }
    }
}
