package com.hevincj.cashflow.ui.screen.state

import com.hevincj.cashflow.domain.models.RecurringExpense

data class SubscriptionManagerUiState(
    val subscriptions: List<RecurringExpense> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null
)
