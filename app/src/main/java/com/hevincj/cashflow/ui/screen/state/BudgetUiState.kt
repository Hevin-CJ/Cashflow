package com.hevincj.cashflow.ui.screen.state

import com.hevincj.cashflow.domain.models.Budget
import java.time.YearMonth

data class BudgetUiState(
    val budgets: List<Budget> = emptyList(),
    val selectedMonth: YearMonth = YearMonth.now(),
    val availableMonths: List<YearMonth> = listOf(YearMonth.now()),
    val isLoading: Boolean = false,
    val error: String? = null
)
