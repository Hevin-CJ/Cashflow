package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.models.TransactionStats
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class GetStatisticsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(year: Int, month: Int): Flow<TransactionStats> {
        return repository.getAllTransactions().map { allTransactions ->
            // 1. Filter transactions to only include the target month/year
            val targetTransactions = allTransactions.filter { transaction ->
                isTargetMonth(transaction.timestamp, year, month)
            }

            // 2. Sort transactions chronologically (newest first)
            val sortedTransactions = targetTransactions.sortedByDescending { it.timestamp }

            // 3. Calculate totals for target month
            val totalIncome = sortedTransactions.filter { it.type == TransactionType.INCOME }.sumOf { kotlin.math.abs(it.amount) }
            val totalExpenses = sortedTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { kotlin.math.abs(it.amount) }

            // 4. Calculate weekly totals for target month
            val weeklyIncome = mutableListOf(0f, 0f, 0f, 0f)
            val weeklyExpenses = mutableListOf(0f, 0f, 0f, 0f)

            sortedTransactions.forEach { transaction ->
                val weekIndex = getWeekIndex(transaction.timestamp)
                val amount = kotlin.math.abs(transaction.amount).toFloat()
                if (transaction.type == TransactionType.INCOME) {
                    weeklyIncome[weekIndex] += amount
                } else if (transaction.type == TransactionType.EXPENSE) {
                    weeklyExpenses[weekIndex] += amount
                }
            }

            TransactionStats(
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                weeklyIncome = weeklyIncome,
                weeklyExpenses = weeklyExpenses,
                recentTransactions = sortedTransactions // Return all transactions to avoid summary mismatch
            )
        }
    }

    private fun isTargetMonth(timestampMs: Long, year: Int, month: Int): Boolean {
        val zoneId = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(timestampMs).atZone(zoneId).toLocalDate()
        return date.year == year && date.monthValue == month
    }

    private fun getWeekIndex(timestampMs: Long): Int {
        val zoneId = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(timestampMs).atZone(zoneId).toLocalDate()
        val dayOfMonth = date.dayOfMonth
        return when (dayOfMonth) {
            in 1..7 -> 0
            in 8..14 -> 1
            in 15..21 -> 2
            else -> 3
        }
    }
}
