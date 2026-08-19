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
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class ProcessRecurringExpensesUseCase @Inject constructor(
    private val recurringRepository: RecurringExpenseRepository,
    private val transactionRepository: TransactionRepository
) {
    private val processingMutex = Mutex()

    suspend operator fun invoke() = processingMutex.withLock {
        withContext(Dispatchers.IO) {
            val zoneId = ZoneId.systemDefault()
            val currentTime = System.currentTimeMillis()
            val currentLocalDate = Instant.ofEpochMilli(currentTime)
                .atZone(zoneId)
                .toLocalDate()

            val recurringExpenses = recurringRepository.getActiveRecurringExpenses()
            val allTransactions = transactionRepository.getAllTransactionsList()

            // In-memory set of unique keys: (recurringIdentifier + localDate)
            val loggedKeys = mutableSetOf<String>()
            for (tx in allTransactions) {
                if (!tx.recurringExpenseId.isNullOrBlank()) {
                    val txLocalDate = Instant.ofEpochMilli(tx.timestamp)
                        .atZone(zoneId)
                        .toLocalDate()
                    loggedKeys.add("${tx.recurringExpenseId}::$txLocalDate")
                }
            }

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

                val assignedRelationId = when {
                    !expense.id.isNullOrBlank() -> expense.id
                    expense.localId > 0 -> expense.localId.toString()
                    !expense.serverId.isNullOrBlank() -> expense.serverId!!
                    else -> null
                }

                while ((nextDueDateLocal.isBefore(currentLocalDate) || nextDueDateLocal.isEqual(currentLocalDate)) && loopGuard < 100) {
                    loopGuard++

                    val nextDueDateMs = nextDueDateLocal.atStartOfDay(zoneId).toInstant().toEpochMilli()

                    // Cross-reference all possible ID representations
                    val isAlreadyLogged = (
                        (!expense.id.isNullOrBlank() && loggedKeys.contains("${expense.id}::$nextDueDateLocal")) ||
                        (expense.localId > 0 && loggedKeys.contains("${expense.localId}::$nextDueDateLocal")) ||
                        (!expense.serverId.isNullOrBlank() && loggedKeys.contains("${expense.serverId}::$nextDueDateLocal"))
                    )

                    if (!isAlreadyLogged) {
                        val cleanTitle = expense.transaction.title
                        val cleanDescription = expense.transaction.description ?: "$cleanTitle subscription"
                        val newTransaction = expense.transaction.copy(
                            id = "",
                            title = cleanTitle,
                            timestamp = nextDueDateMs,
                            description = cleanDescription,
                            isSynced = false,
                            recurringExpenseId = assignedRelationId
                        )
                        loggedTransactions.add(newTransaction)

                        // Immediately register into in-memory deduplication set
                        if (!expense.id.isNullOrBlank()) loggedKeys.add("${expense.id}::$nextDueDateLocal")
                        if (expense.localId > 0) loggedKeys.add("${expense.localId}::$nextDueDateLocal")
                        if (!expense.serverId.isNullOrBlank()) loggedKeys.add("${expense.serverId}::$nextDueDateLocal")
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
                    val finalNextDueDateMs = nextDueDateLocal.atStartOfDay(zoneId).toInstant().toEpochMilli()
                    // Use decoupled billing pointer update without triggering recursive worker loops
                    recurringRepository.updateBillingPointers(
                        expenseId = expense.id,
                        localId = expense.localId,
                        nextDueDate = finalNextDueDateMs,
                        lastProcessedDate = lastProcessed
                    )
                }
            }
        }
    }
}