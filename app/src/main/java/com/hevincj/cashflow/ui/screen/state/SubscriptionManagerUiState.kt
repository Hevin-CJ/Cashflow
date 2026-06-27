package com.hevincj.cashflow.ui.screen.state

import androidx.compose.runtime.Immutable
import com.hevincj.cashflow.domain.models.RecurringExpense
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class SubscriptionManagerUiState(
    val subscriptions: ImmutableList<RecurringExpense> = persistentListOf(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null
)
