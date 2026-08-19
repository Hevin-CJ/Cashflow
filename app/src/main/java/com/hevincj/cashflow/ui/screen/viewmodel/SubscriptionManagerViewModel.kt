package com.hevincj.cashflow.ui.screen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hevincj.cashflow.domain.models.RecurringExpense
import com.hevincj.cashflow.domain.repository.RecurringExpenseRepository
import com.hevincj.cashflow.domain.usecase.AddRecurringExpenseUseCase
import com.hevincj.cashflow.domain.usecase.AddTransactionUseCase
import com.hevincj.cashflow.domain.usecase.DeleteRecurringExpenseUseCase
import com.hevincj.cashflow.domain.usecase.GetRecurringExpensesUseCase
import com.hevincj.cashflow.domain.usecase.ProcessRecurringExpensesUseCase
import com.hevincj.cashflow.data.worker.RecurringExpenseSyncScheduler
import com.hevincj.cashflow.ui.screen.state.SubscriptionManagerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList

@HiltViewModel
class SubscriptionManagerViewModel @Inject constructor(
    private val getRecurringExpensesUseCase: GetRecurringExpensesUseCase,
    private val addRecurringExpenseUseCase: AddRecurringExpenseUseCase,
    private val deleteRecurringExpenseUseCase: DeleteRecurringExpenseUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val repository: RecurringExpenseRepository,
    private val syncScheduler: RecurringExpenseSyncScheduler,
    private val processRecurringExpensesUseCase: ProcessRecurringExpensesUseCase
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val state: StateFlow<SubscriptionManagerUiState> = combine(
        getRecurringExpensesUseCase(),
        _isSyncing,
        _error
    ) { subscriptions, isSyncing, error ->
        SubscriptionManagerUiState(
            subscriptions = subscriptions.toImmutableList(),
            isLoading = false,
            isSyncing = isSyncing,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubscriptionManagerUiState(isLoading = true)
    )

    init {
        sync()
    }

    fun sync() {
        viewModelScope.launch {
            _isSyncing.value = true
            _error.value = null
            val syncError = repository.syncRecurringExpenses()
            if (syncError == null) {
                try {
                    processRecurringExpensesUseCase()
                } catch (e: Exception) {
                    _error.value = e.message ?: "Failed to process recurring transactions"
                }
            } else {
                _error.value = syncError
            }
            _isSyncing.value = false
        }
    }

    fun addSubscription(recurringExpense: RecurringExpense) {
        viewModelScope.launch {
            // 1. Insert subscription metadata profile blueprint first to obtain local row ID
            val localId = addRecurringExpenseUseCase(recurringExpense)
            val assignedId = if (localId > 0) localId.toString() else recurringExpense.id

            // 2. Log the initial payment transaction with recurringExpenseId linked
            val initialTx = recurringExpense.transaction.copy(
                id = "0",
                timestamp = recurringExpense.startDate,
                isSynced = false,
                recurringExpenseId = assignedId
            )
            addTransactionUseCase(initialTx)

            try {
                processRecurringExpensesUseCase()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to compute initial recurrence cycle"
            }
        }
    }

    fun deleteSubscription(recurringExpense: RecurringExpense) {
        viewModelScope.launch {
            deleteRecurringExpenseUseCase(recurringExpense)

            // FIX 3: Re-verify state entries instantly on deletion cleanup passes
            try {
                processRecurringExpensesUseCase()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}