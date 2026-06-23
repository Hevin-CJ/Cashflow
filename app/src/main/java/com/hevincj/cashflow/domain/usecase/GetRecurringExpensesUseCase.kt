package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.models.RecurringExpense
import com.hevincj.cashflow.domain.repository.RecurringExpenseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecurringExpensesUseCase @Inject constructor(
    private val repository: RecurringExpenseRepository
) {
    operator fun invoke(): Flow<List<RecurringExpense>> {
        return repository.getAllRecurringExpenses()
    }
}
