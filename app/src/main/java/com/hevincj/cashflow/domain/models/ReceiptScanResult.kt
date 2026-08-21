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

sealed interface ReceiptAnalysisOutcome {
    data class Success(val result: ReceiptScanResult) : ReceiptAnalysisOutcome
    data class Error(val message: String, val errorType: ReceiptErrorType) : ReceiptAnalysisOutcome
}

enum class ReceiptErrorType {
    NETWORK_ERROR,
    UNREADABLE_IMAGE,
    RATE_LIMITED,
    CONFIG_ERROR,
    SERVER_ERROR,
    UNKNOWN
}
