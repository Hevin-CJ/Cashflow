package com.hevincj.cashflow.ui.screen.state

import androidx.compose.runtime.Immutable
import com.hevincj.cashflow.domain.models.Budget
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.YearMonth

@Immutable
data class BudgetUiState(
    val budgets: ImmutableList<Budget> = persistentListOf(),
    val selectedMonth: YearMonth = YearMonth.now(),
    val availableMonths: ImmutableList<YearMonth> = persistentListOf(YearMonth.now()),
    val isLoading: Boolean = false,
    val error: String? = null
)
