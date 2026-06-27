package com.hevincj.cashflow.domain.models

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class TransactionStats(
    val totalIncome: Double,
    val totalExpenses: Double,
    val weeklyIncome: ImmutableList<Float>,
    val weeklyExpenses: ImmutableList<Float>,
    val recentTransactions: ImmutableList<Transaction>
)
