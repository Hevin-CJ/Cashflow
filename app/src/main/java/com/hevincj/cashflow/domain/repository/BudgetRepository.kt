package com.hevincj.cashflow.domain.repository

import com.hevincj.cashflow.domain.models.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>>
    suspend fun setBudget(budget: Budget)
    suspend fun deleteBudget(category: String, month: Int, year: Int)
}
