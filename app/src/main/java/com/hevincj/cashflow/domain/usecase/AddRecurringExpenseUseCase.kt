package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.models.RecurringExpense
import com.hevincj.cashflow.domain.repository.RecurringExpenseRepository
import javax.inject.Inject

class AddRecurringExpenseUseCase @Inject constructor(
    private val repository: RecurringExpenseRepository
) {
    suspend operator fun invoke(recurringExpense: RecurringExpense) {
        repository.insertRecurringExpense(recurringExpense)
    }
}
