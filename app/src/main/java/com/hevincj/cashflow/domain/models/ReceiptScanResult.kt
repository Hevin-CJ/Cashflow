package com.hevincj.cashflow.domain.models

import androidx.compose.runtime.Immutable

@Immutable
data class ReceiptScanResult(
    val merchant: String,
    val amount: Double,
    val date: String?,
    val category: String,
    val description: String?
)
