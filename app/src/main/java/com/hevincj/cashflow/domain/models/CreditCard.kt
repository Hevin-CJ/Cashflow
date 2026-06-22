package com.hevincj.cashflow.domain.models

data class CreditCard(
    val id: String,
    val balance: Double,
    val cardNumber: String,
    val cardHolder: String,
    val gradientColors: List<Long> // Store color hex values
)
