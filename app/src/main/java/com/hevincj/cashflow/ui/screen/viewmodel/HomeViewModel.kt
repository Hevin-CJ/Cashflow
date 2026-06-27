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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
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

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val repository: TransactionRepository,
    private val networkMonitor: com.hevincj.cashflow.utils.NetworkMonitor,
    private val budgetRepository: BudgetRepository,
    private val processRecurringExpensesUseCase: ProcessRecurringExpensesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var allTransactions: List<Transaction> = emptyList()
    private var budgets: List<com.hevincj.cashflow.domain.models.Budget> = emptyList()
    private var syncJob: kotlinx.coroutines.Job? = null
    private var isInitialSyncRunning = false

    // Cached at class level — ZoneId.systemDefault() performs a timezone database lookup on
    // first call and is expensive. The system timezone never changes while the app process runs.
    private val zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault()

    // For testing dispatcher override
    internal var defaultDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Default

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<TransactionCategory?>(null)
    val selectedCategory: StateFlow<TransactionCategory?> = _selectedCategory.asStateFlow()

    val filteredTransactions: StateFlow<kotlinx.collections.immutable.ImmutableList<Transaction>> = combine(
        state,
        _searchQuery,
        _selectedCategory
    ) { uiState, query, category ->
        withContext(defaultDispatcher) {
            uiState.transactions.filter { tx ->
                val matchesSearch = tx.title.contains(query, ignoreCase = true) ||
                        (tx.description?.contains(query, ignoreCase = true) ?: false)
                val matchesCategory = category == null || tx.category == category
                matchesSearch && matchesCategory
            }.toImmutableList()
        }
    }.stateIn(
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

    /**
     * Combines the transaction flow and the budget flow into a single collector so that
     * [computeTotals] fires exactly once per pair of emissions instead of twice (previously
     * it was called once from loadTransactions() and once from observeBudgets() on every
     * DB update, causing two rapid full recompositions of HomeScreenContent on each sync).
     *
     * Uses a stable-key distinctUntilChanged comparator on the transaction list to suppress
     * spurious re-emissions caused by ImageVector object-identity inequality: two lists whose
     * items share the same id/timestamp/amount/isSynced are treated as equal even if their
     * icon/iconBgColor object references differ after re-mapping from Room.
     */
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun observeCombinedState() {
        val currentMonth = java.time.YearMonth.now()
        viewModelScope.launch {
            combine(
                getTransactionsUseCase()
                    .distinctUntilChanged { old, new ->
                        old.size == new.size &&
                            old.zip(new).all { (o, n) ->
                                o.id == n.id &&
                                    o.title == n.title &&
                                    o.timestamp == n.timestamp &&
                                    o.amount == n.amount &&
                                    o.type == n.type &&
                                    o.category == n.category &&
                                    o.description == n.description &&
                                    o.isSynced == n.isSynced &&
                                    o.barcode == n.barcode &&
                                    o.productName == n.productName
                            }
                    }
                    .debounce(20L),
                budgetRepository.getBudgetsForMonth(
                    currentMonth.monthValue,
                    currentMonth.year
                ).distinctUntilChanged()
            ) { transactions, budgetList ->
                Pair(transactions, budgetList)
            }.collect { (transactions, budgetList) ->
                android.util.Log.d("CashFlowDebug", "Loaded transactions count: ${transactions.size}")
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
                    android.util.Log.d("CashFlowDebug", "Network connectivity state changed: isConnected = $isConnected")
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
            _state.value = _state.value.copy(error = finalError, isLoading = false)
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
            repository.deleteTransaction(transaction)
        }
    }

    // loadTransactions() has been merged into observeCombinedState() above.

    private suspend fun computeTotals() {
        withContext(defaultDispatcher) {
            val range = _state.value.balanceRange
            // Room query already orders by timestamp DESC, so we don't need in-memory sorting
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

            // Compute budget spending
            val currentMonth = java.time.YearMonth.now()
            val currentYear = currentMonth.year
            val currentMonthValue = currentMonth.monthValue
            val exceeded = budgets.map { budget ->
                val spent = sortedTransactions.filter { tx ->
                    tx.type == TransactionType.EXPENSE &&
                    tx.category == budget.category &&
                    isTargetMonth(tx.timestamp, currentYear, currentMonthValue, zoneId)
                }.sumOf { kotlin.math.abs(it.amount) }
                budget.copy(spent = spent)
            }.filter { it.isExceeded }

            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(
                    transactions = sortedTransactions.toImmutableList(),
                    totalIncome = income,
                    totalExpense = expense,
                    totalBalance = balance,
                    exceededBudgets = exceeded.toImmutableList(),
                    // Clear loading once data has arrived. The dual-emission that previously
                    // caused a second rapid recomposition here is now eliminated by combine()
                    // in observeCombinedState, so this fires only once per sync cycle.
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
