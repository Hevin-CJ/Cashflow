package com.hevincj.cashflow.domain.models

import androidx.compose.runtime.Immutable

@Immutable
data class RecurringExpense(
    val id: String,
    val localId: Int = 0, // Fixed invariant tracking index
    val serverId: String? = null,
    val isSynced: Boolean = false,
    val frequency: RecurringFrequency,
    val startDate: Long,
    val lastProcessedDate: Long? = null,
    val nextDueDate: Long,
    val transaction: Transaction
)