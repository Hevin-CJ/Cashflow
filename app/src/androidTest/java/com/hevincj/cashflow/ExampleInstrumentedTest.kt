package com.hevincj.cashflow

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hevincj.cashflow.data.worker.RecurringExpenseWorker.RecurringExpenseWorkerEntryPoint
import com.hevincj.cashflow.domain.models.RecurringExpense
import com.hevincj.cashflow.domain.models.RecurringFrequency
import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.domain.repository.RecurringExpenseRepository
import com.hevincj.cashflow.domain.repository.TransactionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import com.hevincj.cashflow.di.TestEntryPoint

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.hevincj.cashflow", appContext.packageName)
    }

    @Test
    fun testRecurringExpenseProcessing() = runBlocking {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        
        val testEntryPoint = EntryPointAccessors.fromApplication(
            appContext,
            TestEntryPoint::class.java
        )
        val recurringRepository = testEntryPoint.recurringRepository()
        
        val workerEntryPoint = EntryPointAccessors.fromApplication(
            appContext,
            RecurringExpenseWorkerEntryPoint::class.java
        )
        val processUseCase = workerEntryPoint.processRecurringExpensesUseCase()

        // 1. Create a daily subscription with nextDueDate in the past (yesterday)
        val currentTime = System.currentTimeMillis()
        val yesterday = currentTime - 24 * 3600 * 1000L
        
        val expense = RecurringExpense(
            id = "test_sub_123",
            serverId = "test_sub_123",
            isSynced = false,
            frequency = RecurringFrequency.DAILY,
            startDate = yesterday - 1000L,
            lastProcessedDate = null,
            nextDueDate = yesterday,
            transaction = Transaction(
                id = "",
                title = "Integration Test Sub",
                timestamp = yesterday,
                amount = 10.0,
                icon = TransactionCategory.OTHERS.icon,
                iconBgColor = androidx.compose.ui.graphics.Color(0xFF808080),
                type = TransactionType.EXPENSE,
                category = TransactionCategory.OTHERS,
                description = "Test Sub Description",
                isSynced = false
            )
        )

        // Clean up any old test record if exists
        try {
            recurringRepository.deleteRecurringExpense(expense)
        } catch (e: Exception) {}

        // 2. Insert the test subscription
        recurringRepository.insertRecurringExpense(expense)

        // 3. Process recurring expenses
        processUseCase()

        // 4. Retrieve from database and verify it has updated nextDueDate to today/tomorrow
        val activeSubscriptions = recurringRepository.getActiveRecurringExpenses()
        val updatedSub = activeSubscriptions.find { it.serverId == "test_sub_123" }
        
        assertNotNull("Subscription should be in database", updatedSub)
        assertTrue(
            "nextDueDate should be advanced",
            updatedSub!!.nextDueDate > yesterday
        )

        // 5. Clean up
        recurringRepository.deleteRecurringExpense(updatedSub)
    }
}