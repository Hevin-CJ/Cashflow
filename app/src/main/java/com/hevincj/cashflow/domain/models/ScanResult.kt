package com.hevincj.cashflow.domain.models

data class ScanResult(
    val barcode: String?,
    val productName: String,
    val category: String,
    val price: Double?,
    val currency: String?
)
