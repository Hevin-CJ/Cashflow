package com.hevincj.cashflow.ui.screen.state

import com.hevincj.cashflow.domain.models.TransactionStats
import java.time.YearMonth

data class StatsUiState(
    val stats: TransactionStats? = null,
    val selectedMonth: YearMonth = YearMonth.now(),
    val availableMonths: List<YearMonth> = listOf(YearMonth.now()),
    val isLoading: Boolean = false,
    val error: String? = null
)
