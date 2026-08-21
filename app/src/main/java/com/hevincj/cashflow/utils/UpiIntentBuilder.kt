package com.hevincj.cashflow.utils

import android.net.Uri

object UpiIntentBuilder {

    /**
     * Normalises phone number to 10 digits for Indian mobile numbers.
     */
    fun normalisePhone(raw: String): String {
        val digits = raw.replace(Regex("[^0-9]"), "")
        return when {
            digits.startsWith("91") && digits.length == 12 -> digits.substring(2)
            digits.startsWith("0") && digits.length == 11  -> digits.substring(1)
            digits.length == 10                             -> digits
            else                                            -> digits
        }
    }

    /**
     * Resolves the default VPA handle based on package name.
     */
    fun resolveVpaForApp(packageName: String, phone10: String, customHandle: String? = null): String {
        if (!customHandle.isNullOrBlank()) {
            val handle = if (customHandle.startsWith("@")) customHandle else "@$customHandle"
            return "$phone10$handle"
        }

        return when (packageName) {
            "com.phonepe.app" -> "$phone10@ybl"
            "net.one97.paytm" -> "$phone10@paytm"
            "in.org.npci.upiapp" -> "$phone10@upi"
            "in.amazon.mShop.android.shopping" -> "$phone10@apl"
            "com.google.android.apps.nbu.paisa.user" -> "$phone10@okaxis"
            "com.mobikwik_new" -> "$phone10@ikwik"
            else -> "$phone10@upi"
        }
    }

    /**
     * Builds a standard UPI payment URI with parameters.
     */
    fun buildUpiUri(vpa: String, name: String, amount: String? = null, note: String? = null): String {
        val encodedPa = java.net.URLEncoder.encode(vpa, "UTF-8")
        val encodedPn = java.net.URLEncoder.encode(name, "UTF-8")
        val params = StringBuilder("pa=$encodedPa&pn=$encodedPn&cu=INR")

        if (!amount.isNullOrBlank()) {
            val encodedAm = java.net.URLEncoder.encode(amount, "UTF-8")
            params.append("&am=$encodedAm")
        }
        if (!note.isNullOrBlank()) {
            val encodedTn = java.net.URLEncoder.encode(note, "UTF-8")
            params.append("&tn=$encodedTn")
        }

        return "upi://pay?$params"
    }
}
