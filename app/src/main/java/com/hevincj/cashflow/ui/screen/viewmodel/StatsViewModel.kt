package com.hevincj.cashflow.ui.screen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hevincj.cashflow.domain.repository.TransactionRepository
import com.hevincj.cashflow.domain.usecase.GetStatisticsUseCase
import com.hevincj.cashflow.ui.screen.state.StatsUiState
import com.hevincj.cashflow.ui.screen.state.MonthlyNetSavings
import com.hevincj.cashflow.domain.models.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList

import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val getStatisticsUseCase: GetStatisticsUseCase
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    private val _state = MutableStateFlow(StatsUiState(isLoading = true))
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init {
        observeTransactionsAndSelectedMonth()
    }

    fun selectMonth(yearMonth: YearMonth) {
        _selectedMonth.value = yearMonth
    }

    private fun observeTransactionsAndSelectedMonth() {
        viewModelScope.launch {
            _selectedMonth
                // flatMapLatest cancels the previous inner flow automatically when the
                // month changes — eliminates the manual statsJob?.cancel() race window.
                .flatMapLatest { selectedMonth ->
                    // isLoading = true fires ONLY when the user picks a new month,
                    // not on every background DB emission. This eliminates the
                    // "flash blank screen + chart re-animates" bug.
                    _state.value = _state.value.copy(
                        isLoading = true,
                        selectedMonth = selectedMonth
                    )

                    // Combine statistics flow with the live transaction list.
                    // When new transactions arrive from a background sync, the
                    // available-month list updates silently — no isLoading flash.
                    getStatisticsUseCase(selectedMonth.year, selectedMonth.monthValue)
                        .combine(repository.getAllTransactions()) { stats, allTransactions ->
                            val transactionsByMonth = allTransactions.groupBy { tx ->
                                val date = Instant.ofEpochMilli(tx.timestamp)
                                    .atZone(ZoneId.systemDefault()).toLocalDate()
                                YearMonth.of(date.year, date.monthValue)
                            }

                            val months = (transactionsByMonth.keys + YearMonth.now()).distinct().sortedDescending()

                            val netSavingsTrend = months.sorted().map { month ->
                                val monthTransactions = transactionsByMonth[month] ?: emptyList()
                                val income = monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { kotlin.math.abs(it.amount) }
                                val expense = monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { kotlin.math.abs(it.amount) }
                                MonthlyNetSavings(month, income - expense)
                            }

                            StatsUiState(
                                stats = stats,
                                selectedMonth = selectedMonth,
                                availableMonths = months.toImmutableList(),
                                netSavingsTrend = netSavingsTrend.toImmutableList(),
                                isLoading = false
                            )
                        }
                }
                .collect { newState ->
                    _state.value = newState
                }
        }
    }
}
