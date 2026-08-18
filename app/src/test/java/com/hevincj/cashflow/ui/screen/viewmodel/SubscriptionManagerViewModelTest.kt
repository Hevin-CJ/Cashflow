package com.hevincj.cashflow.ui.screen.viewmodel

import com.hevincj.cashflow.MainDispatcherRule
import com.hevincj.cashflow.domain.repository.RecurringExpenseRepository
import com.hevincj.cashflow.domain.usecase.GetRecurringExpensesUseCase
import com.hevincj.cashflow.domain.usecase.AddRecurringExpenseUseCase
import com.hevincj.cashflow.domain.usecase.DeleteRecurringExpenseUseCase
import com.hevincj.cashflow.domain.usecase.ProcessRecurringExpensesUseCase
import com.hevincj.cashflow.data.worker.RecurringExpenseScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionManagerViewModelTest {

    @Rule
    @JvmField
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    lateinit var getRecurringExpensesUseCase: GetRecurringExpensesUseCase

    @Mock
    lateinit var addRecurringExpenseUseCase: AddRecurringExpenseUseCase

    @Mock
    lateinit var deleteRecurringExpenseUseCase: DeleteRecurringExpenseUseCase

    @Mock
    lateinit var addTransactionUseCase: com.hevincj.cashflow.domain.usecase.AddTransactionUseCase

    @Mock
    lateinit var processRecurringExpensesUseCase: ProcessRecurringExpensesUseCase

    @Mock
    lateinit var repository: RecurringExpenseRepository

    @Mock
    lateinit var scheduler: RecurringExpenseScheduler

    private lateinit var viewModel: SubscriptionManagerViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(getRecurringExpensesUseCase.invoke()).thenReturn(flowOf(emptyList()))
    }

    @Test
    fun testInitializationTriggersSyncAndSchedulerCheck() = runTest {
        viewModel = SubscriptionManagerViewModel(
            getRecurringExpensesUseCase,
            addRecurringExpenseUseCase,
            deleteRecurringExpenseUseCase,
            addTransactionUseCase,
            repository,
            scheduler,
            processRecurringExpensesUseCase
        )
        advanceUntilIdle()

        verify(repository).syncRecurringExpenses()
        verify(scheduler).triggerOneTimeCheck()
    }

    @Test
    fun testSyncTriggersSyncAndSchedulerCheck() = runTest {
        viewModel = SubscriptionManagerViewModel(
            getRecurringExpensesUseCase,
            addRecurringExpenseUseCase,
            deleteRecurringExpenseUseCase,
            addTransactionUseCase,
            repository,
            scheduler,
            processRecurringExpensesUseCase
        )
        advanceUntilIdle()

        viewModel.sync()
        advanceUntilIdle()

        verify(repository, times(2)).syncRecurringExpenses()
        verify(scheduler, times(2)).triggerOneTimeCheck()
    }
}
