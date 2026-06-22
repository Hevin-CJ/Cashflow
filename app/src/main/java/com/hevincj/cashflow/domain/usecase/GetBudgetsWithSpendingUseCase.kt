package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.models.Budget
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.domain.repository.BudgetRepository
import com.hevincj.cashflow.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class GetBudgetsWithSpendingUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(month: Int, year: Int): Flow<List<Budget>> {
        val budgetsFlow = budgetRepository.getBudgetsForMonth(month, year)
        val transactionsFlow = transactionRepository.getAllTransactions()

        return combine(budgetsFlow, transactionsFlow) { budgets, transactions ->
            budgets.map { budget ->
                val spent = transactions
                    .filter { transaction ->
                        transaction.type == TransactionType.EXPENSE &&
                                transaction.category == budget.category &&
                                isTargetMonth(transaction.timestamp, year, month)
                    }
                    .sumOf { kotlin.math.abs(it.amount) }

                budget.copy(spent = spent)
            }
        }
    }

    private fun isTargetMonth(timestampMs: Long, year: Int, month: Int): Boolean {
        val zoneId = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(timestampMs).atZone(zoneId).toLocalDate()
        return date.year == year && date.monthValue == month
    }
}
