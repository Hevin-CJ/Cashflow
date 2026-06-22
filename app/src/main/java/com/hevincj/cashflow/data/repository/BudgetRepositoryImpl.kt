package com.hevincj.cashflow.data.repository

import com.hevincj.cashflow.data.local.dao.BudgetDao
import com.hevincj.cashflow.data.mapper.toDomain
import com.hevincj.cashflow.data.mapper.toEntity
import com.hevincj.cashflow.domain.models.Budget
import com.hevincj.cashflow.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val dao: BudgetDao
) : BudgetRepository {

    override fun getBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>> {
        return dao.getBudgetsForMonth(month, year).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun setBudget(budget: Budget) {
        dao.insertBudget(budget.toEntity())
    }

    override suspend fun deleteBudget(category: String, month: Int, year: Int) {
        dao.deleteBudget(category, month, year)
    }
}
