package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.models.RecurringFrequency
import com.hevincj.cashflow.domain.models.RecurringExpense
import com.hevincj.cashflow.domain.repository.RecurringExpenseRepository
import com.hevincj.cashflow.domain.repository.TransactionRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class ProcessRecurringExpensesUseCase @Inject constructor(
    private val recurringRepository: RecurringExpenseRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke() = withContext(Dispatchers.Default) {
        val zoneId = ZoneId.systemDefault()
        val currentTime = System.currentTimeMillis()
        val currentLocalDate = Instant.ofEpochMilli(currentTime)
            .atZone(zoneId)
            .toLocalDate()

        val recurringExpenses = recurringRepository.getActiveRecurringExpenses()
        val allTransactions = transactionRepository.getAllTransactionsList()

        for (expense in recurringExpenses) {
            var nextDueDate = expense.nextDueDate

            // Safety Check: Prevent macro-flooding loops if dates are corrupt or uninitialized
            if (nextDueDate <= 0L) {
                nextDueDate = expense.startDate
            }
            if (nextDueDate < expense.startDate) {
                nextDueDate = expense.startDate
            }

            var nextDueDateLocal = Instant.ofEpochMilli(nextDueDate)
                .atZone(zoneId)
                .toLocalDate()

            val startDateLocal = Instant.ofEpochMilli(expense.startDate)
                .atZone(zoneId)
                .toLocalDate()

            if (nextDueDateLocal.isBefore(startDateLocal)) {
                nextDueDateLocal = startDateLocal
            }

            var lastProcessed = expense.lastProcessedDate
            val loggedTransactions = mutableListOf<Transaction>()
            var dateChanged = false
            var loopGuard = 0 // Absolute breakout threshold to prevent application lockup

            while ((nextDueDateLocal.isBefore(currentLocalDate) || nextDueDateLocal.isEqual(currentLocalDate)) && loopGuard < 100) {
                loopGuard++

                val nextDueDateMs = nextDueDateLocal.atStartOfDay(zoneId).toInstant().toEpochMilli()

                // Cross-reference all valid local/remote keys using stable date normalization
                val isAlreadyLogged = allTransactions.any { tx ->
                    val isMatchingRelation = tx.recurringExpenseId == expense.id ||
                            tx.recurringExpenseId == expense.localId.toString() ||
                            (expense.serverId != null && tx.recurringExpenseId == expense.serverId)

                    val txLocalDate = Instant.ofEpochMilli(tx.timestamp)
                        .atZone(zoneId)
                        .toLocalDate()

                    isMatchingRelation && txLocalDate.isEqual(nextDueDateLocal)
                }

                if (!isAlreadyLogged) {
                    val newTransaction = expense.transaction.copy(
                        id = "",
                        timestamp = nextDueDateMs,
                        description = expense.transaction.description ?: "Auto-logged subscription: ${expense.transaction.title}",
                        isSynced = false,
                        recurringExpenseId = expense.id
                    )
                    loggedTransactions.add(newTransaction)
                }

                lastProcessed = nextDueDateMs
                nextDueDateLocal = when (expense.frequency) {
                    RecurringFrequency.DAILY -> nextDueDateLocal.plusDays(1)
                    RecurringFrequency.WEEKLY -> nextDueDateLocal.plusWeeks(1)
                    RecurringFrequency.MONTHLY -> nextDueDateLocal.plusMonths(1)
                    RecurringFrequency.YEARLY -> nextDueDateLocal.plusYears(1)
                }
                dateChanged = true
            }

            if (loggedTransactions.isNotEmpty()) {
                for (transaction in loggedTransactions) {
                    transactionRepository.insertTransaction(transaction)
                }
            }

            if (dateChanged) {
                val updatedExpense = expense.copy(
                    lastProcessedDate = lastProcessed,
                    nextDueDate = nextDueDateLocal.atStartOfDay(zoneId).toInstant().toEpochMilli()
                )
                recurringRepository.updateRecurringExpense(updatedExpense)
            }
        }
    }
}