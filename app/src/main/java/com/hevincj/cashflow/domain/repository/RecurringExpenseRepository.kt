package com.hevincj.cashflow.domain.repository

import com.hevincj.cashflow.domain.models.RecurringExpense
import kotlinx.coroutines.flow.Flow

interface RecurringExpenseRepository {
    fun getAllRecurringExpenses(): Flow<List<RecurringExpense>>
    suspend fun insertRecurringExpense(recurringExpense: RecurringExpense): Long
    suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense)
    suspend fun getActiveRecurringExpenses(): List<RecurringExpense>
    suspend fun updateRecurringExpense(recurringExpense: RecurringExpense)
    suspend fun updateBillingPointers(expenseId: String, localId: Int, nextDueDate: Long, lastProcessedDate: Long?)
    suspend fun syncRecurringExpenses(): String?
}
