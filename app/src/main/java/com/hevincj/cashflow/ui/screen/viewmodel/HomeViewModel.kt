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
import com.hevincj.cashflow.domain.usecase.DeleteTransactionUseCase
import com.hevincj.cashflow.ui.screen.state.HomeUiState
import com.hevincj.cashflow.ui.screen.state.BalanceRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.usecase.ProcessRecurringExpensesUseCase
import com.hevincj.cashflow.domain.repository.TransactionRepository
import com.hevincj.cashflow.domain.repository.BudgetRepository
import java.time.ZoneId

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val repository: TransactionRepository,
    private val networkMonitor: com.hevincj.cashflow.utils.NetworkMonitor,
    private val budgetRepository: BudgetRepository,
    private val processRecurringExpensesUseCase: ProcessRecurringExpensesUseCase,
    private val authRepository: com.hevincj.cashflow.domain.repository.AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var allTransactions: List<Transaction> = emptyList()
    private var budgets: List<com.hevincj.cashflow.domain.models.Budget> = emptyList()
    private var syncJob: kotlinx.coroutines.Job? = null
    private var isInitialSyncRunning = false

    private val zoneId: ZoneId = ZoneId.systemDefault()

    internal var defaultDispatcher: kotlinx.coroutines.CoroutineDispatcher =
        testDispatcherOverride ?: kotlinx.coroutines.Dispatchers.Default

    companion object {
        var testDispatcherOverride: kotlinx.coroutines.CoroutineDispatcher? = null
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<TransactionCategory?>(null)
    val selectedCategory: StateFlow<TransactionCategory?> = _selectedCategory.asStateFlow()

    @OptIn(FlowPreview::class)
    val filteredTransactions: StateFlow<kotlinx.collections.immutable.ImmutableList<Transaction>> = combine(
        state.map { it.transactions }.distinctUntilChanged().debounce(100L),
        _searchQuery,
        _selectedCategory
    ) { transactions, query, category ->
        transactions.filter { tx ->
            val matchesSearch = tx.title.contains(query, ignoreCase = true) ||
                    (tx.description?.contains(query, ignoreCase = true) ?: false)
            val matchesCategory = category == null || tx.category == category
            matchesSearch && matchesCategory
        }.toImmutableList()
    }
    .flowOn(defaultDispatcher)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = persistentListOf()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: TransactionCategory?) {
        _selectedCategory.value = category
    }

    init {
        observeCombinedState()
        observeNetworkChanges()
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun observeCombinedState() {
        val currentMonth = java.time.YearMonth.now()
        viewModelScope.launch {
            combine(
                getTransactionsUseCase().distinctUntilChanged(),
                budgetRepository.getBudgetsForMonth(
                    currentMonth.monthValue,
                    currentMonth.year
                ).distinctUntilChanged()
            ) { transactions, budgetList ->
                Pair(transactions, budgetList)
            }
                .debounce(20L)
                .flowOn(defaultDispatcher)
                .collect { (transactions, budgetList) ->
                    allTransactions = transactions
                    budgets = budgetList
                    computeTotals()
                }
        }
    }

    private fun observeNetworkChanges() {
        viewModelScope.launch {
            networkMonitor.isConnected
                .distinctUntilChanged()
                .collect { isConnected ->
                    if (isConnected) {
                        val currentError = _state.value.error
                        if (currentError != null) {
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
            try {
                processRecurringExpensesUseCase()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val syncError = repository.syncTransactions(limit)
            val budgetSyncError = budgetRepository.syncBudgets()
            isInitialSyncRunning = false
            val finalError = syncError ?: budgetSyncError
            val isSessionExpired = finalError?.contains("code 401") == true
            if (isSessionExpired) {
                authRepository.logout()
                _state.value = _state.value.copy(
                    error = "Session expired. Please log in again.",
                    isLoading = false,
                    isSessionExpired = true
                )
            } else {
                _state.value = _state.value.copy(error = finalError, isLoading = false)
            }
        }
    }

    fun onBalanceRangeChange(range: BalanceRange) {
        _state.value = _state.value.copy(balanceRange = range)
        viewModelScope.launch {
            computeTotals()
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            deleteTransactionUseCase(transaction)
        }
    }

    private suspend fun computeTotals() {
        withContext(defaultDispatcher) {
            val range = _state.value.balanceRange
            val sortedTransactions = allTransactions

            val filteredForTotals = when (range) {
                BalanceRange.ALL_TIME -> sortedTransactions
                BalanceRange.THIS_MONTH -> {
                    val currentYearMonth = java.time.YearMonth.now()
                    sortedTransactions.filter { tx ->
                        val txYearMonth = java.time.YearMonth.from(
                            java.time.Instant.ofEpochMilli(tx.timestamp)
                                .atZone(zoneId)
                                .toLocalDate()
                        )
                        txYearMonth == currentYearMonth
                    }
                }
                BalanceRange.THIS_YEAR -> {
                    val currentYear = java.time.Year.now().value
                    sortedTransactions.filter { tx ->
                        val txYear = java.time.Instant.ofEpochMilli(tx.timestamp)
                            .atZone(zoneId)
                            .toLocalDate()
                            .year
                        txYear == currentYear
                    }
                }
            }

            val income = filteredForTotals.filter { it.type == TransactionType.INCOME }.sumOf { kotlin.math.abs(it.amount) }
            val expense = filteredForTotals.filter { it.type == TransactionType.EXPENSE }.sumOf { kotlin.math.abs(it.amount) }
            val balance = income - expense

            val currentMonth = java.time.YearMonth.now()
            val currentYear = currentMonth.year
            val currentMonthValue = currentMonth.monthValue

            val categorySpendingMap = sortedTransactions
                .filter { it.type == TransactionType.EXPENSE && isTargetMonth(it.timestamp, currentYear, currentMonthValue, zoneId) }
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { kotlin.math.abs(it.amount) } }

            val exceeded = budgets.map { budget ->
                val spent = categorySpendingMap[budget.category] ?: 0.0
                budget.copy(spent = spent)
            }.filter { it.isExceeded }

            // Slice list cleanly to maximum display elements here on background thread
            val displayTransactions = sortedTransactions.take(25).toImmutableList()

            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(
                    transactions = displayTransactions,
                    totalIncome = income,
                    totalExpense = expense,
                    totalBalance = balance,
                    exceededBudgets = exceeded.toImmutableList(),
                    isLoading = if (sortedTransactions.isNotEmpty()) false else _state.value.isLoading
                )
            }
        }
    }

    private fun isTargetMonth(timestampMs: Long, year: Int, month: Int, zone: java.time.ZoneId): Boolean {
        val date = java.time.Instant.ofEpochMilli(timestampMs).atZone(zone).toLocalDate()
        return date.year == year && date.monthValue == month
    }
}