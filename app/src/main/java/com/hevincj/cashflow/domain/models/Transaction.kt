package com.hevincj.cashflow.domain.models


import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
data class Transaction(
    val id: String,
    val title: String,
    val timestamp: Long,
    val amount: Double,
    val icon: ImageVector,
    val iconBgColor: Color,
    val type: TransactionType,
    val category: TransactionCategory,
    val description: String?,
    val isSynced: Boolean = false,
    val barcode: String? = null,
    val productName: String? = null,
    val formattedDate: String = "",
    val lastModifiedLocal: Long = 0L,
    val recurringExpenseId: String? = null
)