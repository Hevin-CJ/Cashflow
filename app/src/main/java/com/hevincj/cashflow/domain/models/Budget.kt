package com.hevincj.cashflow.domain.models

data class Budget(
    val category: TransactionCategory,
    val monthlyLimit: Double,
    val spent: Double,       // derived / calculated
    val month: Int,
    val year: Int
) {
    val progress: Float get() = if (monthlyLimit > 0) (spent / monthlyLimit).toFloat().coerceIn(0f, 1f) else 0f
    val isWarning: Boolean get() = progress >= 0.8f && progress < 1.0f
    val isExceeded: Boolean get() = progress >= 1.0f
}
