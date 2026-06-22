package com.hevincj.cashflow.data.remote.models

data class ScanResultDto(
    val barcode: String?,
    val productName: String,
    val category: String,
    val price: Double?,
    val currency: String?
)

data class ReceiptScanResponseDto(
    val totalAmount: Double,
    val category: String,
    val description: String,
    val currency: String
)

data class UpiQrResponseDto(
    val upiUri: String,
    val qrCodeUrl: String
)
