package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.models.Budget
import com.hevincj.cashflow.domain.repository.BudgetRepository
import javax.inject.Inject

class SetBudgetUseCase @Inject constructor(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(budget: Budget) {
        repository.setBudget(budget)
    }
}
