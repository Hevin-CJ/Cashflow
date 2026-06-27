package com.hevincj.cashflow.ui.screen.state

import androidx.compose.runtime.Immutable
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.domain.models.TransactionCategory

@Immutable
data class AddTransactionUiState(
    val title: String = "",
    val amount: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val category: TransactionCategory = TransactionCategory.FOOD,
    val description: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val isEditMode: Boolean = false,
    val transactionId: String? = null
)
