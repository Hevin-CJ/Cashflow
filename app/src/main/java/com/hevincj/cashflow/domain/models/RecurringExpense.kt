package com.hevincj.cashflow.domain.models

data class RecurringExpense(
    val id: String,
    val serverId: String? = null,
    val isSynced: Boolean = false,
    val title: String,
    val amount: Double,
    val category: TransactionCategory,
    val type: TransactionType,
    val frequency: String,
    val startDate: Long,
    val lastProcessedDate: Long? = null,
    val nextDueDate: Long,
    val description: String? = null
)
