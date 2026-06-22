package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.repository.BudgetRepository
import javax.inject.Inject

class DeleteBudgetUseCase @Inject constructor(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(category: String, month: Int, year: Int) {
        repository.deleteBudget(category, month, year)
    }
}
