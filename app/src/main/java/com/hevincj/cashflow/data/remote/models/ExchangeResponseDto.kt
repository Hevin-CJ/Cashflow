package com.hevincj.cashflow.data.remote.models

data class ExchangeResponseDto(
    val amount: Double,
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)
