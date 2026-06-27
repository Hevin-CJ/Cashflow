package com.hevincj.cashflow.domain.models

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class CreditCard(
    val id: String,
    val balance: Double,
    val cardNumber: String,
    val cardHolder: String,
    val gradientColors: ImmutableList<Long> // Store color hex values
)
