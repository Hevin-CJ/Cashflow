package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.models.RecurringExpense
import com.hevincj.cashflow.domain.repository.RecurringExpenseRepository
import javax.inject.Inject

class DeleteRecurringExpenseUseCase @Inject constructor(
    private val repository: RecurringExpenseRepository
) {
    suspend operator fun invoke(recurringExpense: RecurringExpense) {
        repository.deleteRecurringExpense(recurringExpense)
    }
}
