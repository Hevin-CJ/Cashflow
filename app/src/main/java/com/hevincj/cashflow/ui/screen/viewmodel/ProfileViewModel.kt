package com.hevincj.cashflow.ui.screen.viewmodel

import androidx.lifecycle.ViewModel
import com.hevincj.cashflow.domain.repository.AuthRepository
import com.hevincj.cashflow.data.local.ThemeManager
import com.hevincj.cashflow.data.local.ThemeMode
import com.hevincj.cashflow.domain.repository.TransactionRepository
import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.ui.screen.state.ProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val themeManager: ThemeManager,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            themeManager.themeMode.collect { mode ->
                _state.value = _state.value.copy(themeMode = mode)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        themeManager.setThemeMode(mode)
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _state.value = _state.value.copy(isLoggedOut = true)
        }
    }

    suspend fun getTransactionsForRange(range: ExportDateRange): List<Transaction> {
        val allTransactions = transactionRepository.getAllTransactionsList()
        if (range == ExportDateRange.ALL) return allTransactions

        val zoneId = java.time.ZoneId.systemDefault()
        val now = java.time.LocalDate.now(zoneId)
        val currentMonth = java.time.YearMonth.from(now)
        val currentYear = java.time.Year.from(now)

        val (startMs, endMs) = when (range) {
            ExportDateRange.CURRENT_MONTH -> {
                val start = currentMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val end = currentMonth.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                start to end
            }
            ExportDateRange.PREVIOUS_MONTH -> {
                val prevMonth = currentMonth.minusMonths(1)
                val start = prevMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val end = currentMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                start to end
            }
            ExportDateRange.CURRENT_YEAR -> {
                val start = currentYear.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val end = currentYear.plusYears(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                start to end
            }
            ExportDateRange.PREVIOUS_YEAR -> {
                val prevYear = currentYear.minusYears(1)
                val start = prevYear.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val end = currentYear.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                start to end
            }
            ExportDateRange.ALL -> 0L to 0L
        }

        return allTransactions.filter { it.timestamp in startMs until endMs }
    }
}

enum class ExportDateRange {
    CURRENT_MONTH,
    PREVIOUS_MONTH,
    CURRENT_YEAR,
    PREVIOUS_YEAR,
    ALL
}
