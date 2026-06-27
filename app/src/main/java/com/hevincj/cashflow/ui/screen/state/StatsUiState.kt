package com.hevincj.cashflow.ui.screen.state

import androidx.compose.runtime.Immutable
import com.hevincj.cashflow.domain.models.TransactionStats
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.YearMonth

@Immutable
data class MonthlyNetSavings(
    val month: YearMonth,
    val amount: Double
)

@Immutable
data class StatsUiState(
    val stats: TransactionStats? = null,
    val selectedMonth: YearMonth = YearMonth.now(),
    val availableMonths: ImmutableList<YearMonth> = persistentListOf(YearMonth.now()),
    val netSavingsTrend: ImmutableList<MonthlyNetSavings> = persistentListOf(),
    val isLoading: Boolean = false,
    val error: String? = null
)
