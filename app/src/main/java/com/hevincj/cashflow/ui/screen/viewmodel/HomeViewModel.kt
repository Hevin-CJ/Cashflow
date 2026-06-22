package com.hevincj.cashflow.ui.screen.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.domain.usecase.AddTransactionUseCase
import com.hevincj.cashflow.domain.usecase.GetTransactionsUseCase
import com.hevincj.cashflow.ui.screen.state.HomeUiState
import com.hevincj.cashflow.ui.screen.state.BalanceRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

import com.hevincj.cashflow.domain.repository.TransactionRepository

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val repository: TransactionRepository,
    private val networkMonitor: com.hevincj.cashflow.utils.NetworkMonitor
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var allTransactions: List<Transaction> = emptyList()
    private var syncJob: kotlinx.coroutines.Job? = null
    private var isInitialSyncRunning = false

    init {
        loadTransactions()
        observeNetworkChanges()
    }

    private fun observeNetworkChanges() {
        viewModelScope.launch {
            networkMonitor.isConnected
                .distinctUntilChanged()
                .collect { isConnected ->
                    android.util.Log.d("CashFlowDebug", "Network connectivity state changed: isConnected = $isConnected")
                    if (isConnected) {
                        val currentError = _state.value.error
                        if (currentError == "No internet connection" ||
                            currentError == "Failed to connect to the server." ||
                            currentError == "Connection timed out." ||
                            currentError == "Server is unreachable. Please try again later."
                        ) {
                            _state.value = _state.value.copy(error = null)
                            refreshSync()
                        }
                    } else {
                        _state.value = _state.value.copy(
                            error = "No internet connection",
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun refreshSync(force: Boolean = true, limit: Int = 25) {
        if (syncJob?.isActive == true && !force) return
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            if (allTransactions.isEmpty()) {
                isInitialSyncRunning = true
                _state.value = _state.value.copy(isLoading = true)
            }
            val syncError = repository.syncTransactions(limit)
            isInitialSyncRunning = false
            _state.value = _state.value.copy(error = syncError, isLoading = false)
        }
    }

    fun onBalanceRangeChange(range: BalanceRange) {
        _state.value = _state.value.copy(balanceRange = range)
        updateTotals()
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            getTransactionsUseCase().collect { transactions ->
                android.util.Log.d("CashFlowDebug", "Loaded transactions count: ${transactions.size}")
                allTransactions = transactions
                // Update totals inline — no nested launch needed; collect{} already
                // runs inside the enclosing viewModelScope coroutine.
                updateTotals()
            }
        }
    }

    private fun updateTotals() {
        val range = _state.value.balanceRange
        val sortedTransactions = allTransactions.sortedByDescending { it.timestamp }

        val filteredForTotals = when (range) {
            BalanceRange.ALL_TIME -> sortedTransactions
            BalanceRange.THIS_MONTH -> {
                val currentYearMonth = java.time.YearMonth.now()
                sortedTransactions.filter { tx ->
                    val txYearMonth = java.time.YearMonth.from(
                        java.time.Instant.ofEpochMilli(tx.timestamp)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                    )
                    txYearMonth == currentYearMonth
                }
            }
            BalanceRange.THIS_YEAR -> {
                val currentYear = java.time.Year.now().value
                sortedTransactions.filter { tx ->
                    val txYear = java.time.Instant.ofEpochMilli(tx.timestamp)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                        .year
                    txYear == currentYear
                }
            }
        }

        val income = filteredForTotals.filter { it.type == TransactionType.INCOME }.sumOf { kotlin.math.abs(it.amount) }
        val expense = filteredForTotals.filter { it.type == TransactionType.EXPENSE }.sumOf { kotlin.math.abs(it.amount) }
        val balance = income - expense

        _state.value = _state.value.copy(
            transactions = sortedTransactions,
            totalIncome = income,
            totalExpense = expense,
            totalBalance = balance,
            isLoading = if (sortedTransactions.isNotEmpty()) false else _state.value.isLoading
        )
    }
}
