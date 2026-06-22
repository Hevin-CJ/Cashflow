package com.hevincj.cashflow.domain.models

data class TransactionStats(
    val totalIncome: Double,
    val totalExpenses: Double,
    val weeklyIncome: List<Float>,
    val weeklyExpenses: List<Float>,
    val recentTransactions: List<Transaction>
)
