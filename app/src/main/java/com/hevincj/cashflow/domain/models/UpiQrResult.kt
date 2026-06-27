package com.hevincj.cashflow.domain.models

import androidx.compose.runtime.Immutable

@Immutable
data class UpiQrResult(
    val upiUri: String,
    val qrCodeUrl: String
)
