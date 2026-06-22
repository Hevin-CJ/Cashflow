package com.hevincj.cashflow.utils

fun isProductValid(name: String?, barcode: String): Boolean {
    if (name.isNullOrBlank()) return false
    val trimmed = name.trim()
    val lower = trimmed.lowercase()
    if (lower == barcode.lowercase()) return false
    if (lower == "barcode item") return false
    if (lower == "unknown product") return false
    if (lower.startsWith("barcode item ") || lower.startsWith("barcode item:")) return false
    if (lower.startsWith("unknown product ") || lower.startsWith("unknown product:")) return false
    return true
}
