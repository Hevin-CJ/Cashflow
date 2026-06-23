package com.hevincj.cashflow.ui.screen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hevincj.cashflow.domain.models.RecurringExpense
import com.hevincj.cashflow.domain.repository.RecurringExpenseRepository
import com.hevincj.cashflow.domain.usecase.AddRecurringExpenseUseCase
import com.hevincj.cashflow.domain.usecase.DeleteRecurringExpenseUseCase
import com.hevincj.cashflow.domain.usecase.GetRecurringExpensesUseCase
import com.hevincj.cashflow.data.worker.RecurringExpenseScheduler
import com.hevincj.cashflow.ui.screen.state.SubscriptionManagerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionManagerViewModel @Inject constructor(
    private val getRecurringExpensesUseCase: GetRecurringExpensesUseCase,
    private val addRecurringExpenseUseCase: AddRecurringExpenseUseCase,
    private val deleteRecurringExpenseUseCase: DeleteRecurringExpenseUseCase,
    private val repository: RecurringExpenseRepository,
    private val scheduler: RecurringExpenseScheduler
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val state: StateFlow<SubscriptionManagerUiState> = combine(
        getRecurringExpensesUseCase(),
        _isSyncing,
        _error
    ) { subscriptions, isSyncing, error ->
        SubscriptionManagerUiState(
            subscriptions = subscriptions,
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
            _error.value = syncError
            _isSyncing.value = false

            scheduler.triggerOneTimeCheck()
        }
    }

    fun addSubscription(recurringExpense: RecurringExpense) {
        viewModelScope.launch {
            addRecurringExpenseUseCase(recurringExpense)
        }
    }

    fun deleteSubscription(recurringExpense: RecurringExpense) {
        viewModelScope.launch {
            deleteRecurringExpenseUseCase(recurringExpense)
        }
    }
}
