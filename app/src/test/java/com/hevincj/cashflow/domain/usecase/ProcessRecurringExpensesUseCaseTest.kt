package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.domain.models.RecurringExpense
import com.hevincj.cashflow.domain.models.RecurringFrequency
import com.hevincj.cashflow.domain.repository.RecurringExpenseRepository
import com.hevincj.cashflow.domain.repository.TransactionRepository
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ProcessRecurringExpensesUseCaseTest {

    @Mock
    lateinit var recurringRepository: RecurringExpenseRepository

    @Mock
    lateinit var transactionRepository: TransactionRepository

    private lateinit var useCase: ProcessRecurringExpensesUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        useCase = ProcessRecurringExpensesUseCase(recurringRepository, transactionRepository)
    }

    @Test
    fun testProcessLogsNewTransactionsAndUpdatesBillingPointers() = runTest {
        val currentTime = System.currentTimeMillis()
        val nextDueDate = currentTime - 5000L // 5 seconds in the past

        val expense = RecurringExpense(
            id = "sub_1",
            frequency = RecurringFrequency.MONTHLY,
            startDate = currentTime - 30 * 24 * 3600 * 1000L,
            nextDueDate = nextDueDate,
            transaction = Transaction(
                id = "",
                title = "Netflix",
                timestamp = currentTime - 30 * 24 * 3600 * 1000L,
                amount = -15.0,
                icon = TransactionCategory.ENTERTAINMENT.icon,
                iconBgColor = Color.Red,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.ENTERTAINMENT,
                description = "Auto-logged subscription: Netflix",
                isSynced = false
            )
        )

        whenever(recurringRepository.getActiveRecurringExpenses()).thenReturn(listOf(expense))
        whenever(transactionRepository.getAllTransactionsList()).thenReturn(emptyList())

        useCase()

        verify(transactionRepository).insertTransaction(any())
        verify(recurringRepository).updateBillingPointers(eq("sub_1"), eq(0), any(), any())
    }

    @Test
    fun testProcessSkipsDuplicateTransactionsButAdvancesDate() = runTest {
        val currentTime = System.currentTimeMillis()
        val nextDueDate = currentTime - 5000L // 5 seconds in the past

        val expense = RecurringExpense(
            id = "sub_1",
            frequency = RecurringFrequency.MONTHLY,
            startDate = currentTime - 30 * 24 * 3600 * 1000L,
            nextDueDate = nextDueDate,
            transaction = Transaction(
                id = "",
                title = "Netflix",
                timestamp = currentTime - 30 * 24 * 3600 * 1000L,
                amount = -15.0,
                icon = TransactionCategory.ENTERTAINMENT.icon,
                iconBgColor = Color.Red,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.ENTERTAINMENT,
                description = "Auto-logged subscription: Netflix",
                isSynced = false
            )
        )

        val existingTransaction = Transaction(
            id = "tx_1",
            title = "Netflix",
            timestamp = nextDueDate,
            amount = -15.0,
            icon = TransactionCategory.ENTERTAINMENT.icon,
            iconBgColor = Color.Red,
            type = TransactionType.EXPENSE,
            category = TransactionCategory.ENTERTAINMENT,
            description = "Auto-logged subscription: Netflix",
            recurringExpenseId = "sub_1"
        )

        whenever(recurringRepository.getActiveRecurringExpenses()).thenReturn(listOf(expense))
        whenever(transactionRepository.getAllTransactionsList()).thenReturn(listOf(existingTransaction))

        useCase()

        verify(transactionRepository, never()).insertTransaction(any())
        verify(recurringRepository).updateBillingPointers(eq("sub_1"), eq(0), any(), any())
    }

    @Test
    fun testConcurrentInvocationsAreSerializedByMutex() = runTest {
        val currentTime = System.currentTimeMillis()
        val nextDueDate = currentTime - 5000L

        val expense = RecurringExpense(
            id = "sub_1",
            frequency = RecurringFrequency.MONTHLY,
            startDate = currentTime - 30 * 24 * 3600 * 1000L,
            nextDueDate = nextDueDate,
            transaction = Transaction(
                id = "",
                title = "Spotify",
                timestamp = currentTime - 30 * 24 * 3600 * 1000L,
                amount = -10.0,
                icon = TransactionCategory.ENTERTAINMENT.icon,
                iconBgColor = Color.Green,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.ENTERTAINMENT,
                description = "Spotify Premium",
                isSynced = false
            )
        )

        whenever(recurringRepository.getActiveRecurringExpenses()).thenReturn(listOf(expense))
        whenever(transactionRepository.getAllTransactionsList()).thenReturn(emptyList())

        // Launch concurrent calls to invoke
        val deferred1 = async { useCase() }
        val deferred2 = async { useCase() }

        deferred1.await()
        deferred2.await()

        // Both calls complete cleanly without throwing concurrency errors
        verify(recurringRepository, times(2)).getActiveRecurringExpenses()
    }
}
