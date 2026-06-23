package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.repository.RecurringExpenseRepository
import com.hevincj.cashflow.domain.repository.TransactionRepository
import java.util.Calendar
import javax.inject.Inject

class ProcessRecurringExpensesUseCase @Inject constructor(
    private val recurringRepository: RecurringExpenseRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke() {
        val currentTime = System.currentTimeMillis()
        val recurringExpenses = recurringRepository.getActiveRecurringExpenses()

        for (expense in recurringExpenses) {
            var nextDueDate = expense.nextDueDate
            var lastProcessed = expense.lastProcessedDate
            val loggedTransactions = mutableListOf<Transaction>()

            while (nextDueDate <= currentTime) {
                val newTransaction = Transaction(
                    id = "",
                    title = expense.title,
                    timestamp = nextDueDate,
                    amount = expense.amount,
                    icon = expense.category.icon,
                    iconBgColor = expense.category.iconBgColor,
                    type = expense.type,
                    category = expense.category,
                    description = expense.description ?: "Auto-logged subscription: ${expense.title}",
                    isSynced = false
                )
                loggedTransactions.add(newTransaction)

                lastProcessed = nextDueDate
                nextDueDate = calculateNextDueDate(nextDueDate, expense.frequency)
            }

            if (loggedTransactions.isNotEmpty()) {
                for (transaction in loggedTransactions) {
                    transactionRepository.insertTransaction(transaction)
                }

                val updatedExpense = expense.copy(
                    lastProcessedDate = lastProcessed,
                    nextDueDate = nextDueDate
                )
                recurringRepository.updateRecurringExpense(updatedExpense)
            }
        }
    }

    private fun calculateNextDueDate(currentDueDate: Long, frequency: String): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentDueDate
        }
        when (frequency.uppercase(java.util.Locale.ROOT)) {
            "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
            "YEARLY" -> calendar.add(Calendar.YEAR, 1)
            else -> calendar.add(Calendar.MONTH, 1)
        }
        return calendar.timeInMillis
    }
}
