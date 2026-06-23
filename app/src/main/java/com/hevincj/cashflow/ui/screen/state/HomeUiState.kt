package com.hevincj.cashflow.ui.screen.state

import androidx.compose.runtime.Immutable
import com.hevincj.cashflow.domain.models.Budget
import com.hevincj.cashflow.domain.models.Transaction

enum class BalanceRange(val displayName: String) {
    ALL_TIME("Total Balance"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year")
}

@Immutable
data class HomeUiState(
    val transactions: List<Transaction> = emptyList(),
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balanceRange: BalanceRange = BalanceRange.ALL_TIME,
    val isLoading: Boolean = true,
    val error: String? = null,
    val exceededBudgets: List<Budget> = emptyList()
)
