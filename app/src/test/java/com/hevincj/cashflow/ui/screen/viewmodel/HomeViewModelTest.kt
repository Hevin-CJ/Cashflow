package com.hevincj.cashflow.ui.screen.viewmodel

import com.hevincj.cashflow.MainDispatcherRule
import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.domain.repository.TransactionRepository
import com.hevincj.cashflow.domain.usecase.AddTransactionUseCase
import com.hevincj.cashflow.domain.usecase.GetTransactionsUseCase
import com.hevincj.cashflow.ui.screen.state.BalanceRange
import com.hevincj.cashflow.utils.NetworkMonitor
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.AccountBalance
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import org.mockito.kotlin.times

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    lateinit var getTransactionsUseCase: GetTransactionsUseCase

    @Mock
    lateinit var addTransactionUseCase: AddTransactionUseCase

    @Mock
    lateinit var repository: TransactionRepository

    @Mock
    lateinit var networkMonitor: NetworkMonitor

    @Mock
    lateinit var getBudgetsWithSpendingUseCase: com.hevincj.cashflow.domain.usecase.GetBudgetsWithSpendingUseCase

    @Mock
    lateinit var budgetRepository: com.hevincj.cashflow.domain.repository.BudgetRepository

    private lateinit var viewModel: HomeViewModel

    private val sampleTransactions = listOf(
        Transaction(
            id = "1",
            title = "Groceries",
            timestamp = System.currentTimeMillis(),
            amount = -900.0,
            icon = androidx.compose.material.icons.Icons.Rounded.ShoppingBag,
            iconBgColor = Color(0xFFF19E79),
            type = TransactionType.EXPENSE,
            category = TransactionCategory.GROCERIES,
            description = "Week food shopping",
            isSynced = true
        ),
        Transaction(
            id = "2",
            title = "Salary",
            timestamp = System.currentTimeMillis(),
            amount = 5000.0,
            icon = androidx.compose.material.icons.Icons.Rounded.AccountBalance,
            iconBgColor = Color(0xFF67E2AE),
            type = TransactionType.INCOME,
            category = TransactionCategory.SALARY,
            description = "Monthly pay",
            isSynced = true
        )
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(getTransactionsUseCase.invoke()).thenReturn(flowOf(sampleTransactions))
        whenever(networkMonitor.isConnected).thenReturn(flowOf(true))
        whenever(getBudgetsWithSpendingUseCase.invoke(any(), any())).thenReturn(flowOf(emptyList()))
        runBlocking {
            whenever(budgetRepository.syncBudgets()).thenReturn(null)
        }
    }

    @Test
    fun testInitializationLoadsTransactionsAndCalculatesTotals() = runTest {
        viewModel = HomeViewModel(
            getTransactionsUseCase,
            addTransactionUseCase,
            repository,
            networkMonitor,
            getBudgetsWithSpendingUseCase,
            budgetRepository
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.transactions.size)
        assertEquals(5000.0, state.totalIncome, 0.01)
        assertEquals(900.0, state.totalExpense, 0.01)
        assertEquals(4100.0, state.totalBalance, 0.01)
        assertFalse(state.isLoading)
    }

    @Test
    fun testOnBalanceRangeChangeFiltersTotals() = runTest {
        viewModel = HomeViewModel(
            getTransactionsUseCase,
            addTransactionUseCase,
            repository,
            networkMonitor,
            getBudgetsWithSpendingUseCase,
            budgetRepository
        )
        advanceUntilIdle()
        
        viewModel.onBalanceRangeChange(BalanceRange.ALL_TIME)
        
        val state = viewModel.state.value
        assertEquals(BalanceRange.ALL_TIME, state.balanceRange)
        assertEquals(5000.0, state.totalIncome, 0.01)
        assertEquals(900.0, state.totalExpense, 0.01)
    }

    @Test
    fun testObserveNetworkChangesHandlesOffline() = runTest {
        whenever(networkMonitor.isConnected).thenReturn(flowOf(false))
        val localViewModel = HomeViewModel(
            getTransactionsUseCase,
            addTransactionUseCase,
            repository,
            networkMonitor,
            getBudgetsWithSpendingUseCase,
            budgetRepository
        )
        
        advanceUntilIdle()
        
        assertEquals("No internet connection", localViewModel.state.value.error)
        assertFalse(localViewModel.state.value.isLoading)
    }

    @Test
    fun testObserveNetworkChangesHandlesReconnection() = runTest {
        val connectionFlow = kotlinx.coroutines.flow.MutableStateFlow(false)
        whenever(networkMonitor.isConnected).thenReturn(connectionFlow)
        
        val localViewModel = HomeViewModel(
            getTransactionsUseCase,
            addTransactionUseCase,
            repository,
            networkMonitor,
            getBudgetsWithSpendingUseCase,
            budgetRepository
        )
        
        advanceUntilIdle()
        
        // Starts offline
        assertEquals("No internet connection", localViewModel.state.value.error)
        
        // Reconnects -> error should be cleared and sync triggered automatically
        connectionFlow.value = true
        advanceUntilIdle()
        
        assertEquals(null, localViewModel.state.value.error)
        org.mockito.Mockito.verify(repository).syncTransactions(25)
    }

    @Test
    fun testInitializationDisplaysCachedDataDuringSync() = runTest {
        whenever(repository.syncTransactions(25)).thenReturn(null)

        viewModel = HomeViewModel(
            getTransactionsUseCase,
            addTransactionUseCase,
            repository,
            networkMonitor,
            getBudgetsWithSpendingUseCase,
            budgetRepository
        )
        viewModel.refreshSync(force = true)
        
        val state = viewModel.state.value
        assertEquals(2, state.transactions.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun testRetrySyncsAndClearsErrorAndSyncsToRemote() = runTest {
        // 1. Initial state: sync fails with an error
        whenever(repository.syncTransactions(25)).thenReturn("Failed to sync transactions")
        runBlocking {
            whenever(budgetRepository.syncBudgets()).thenReturn("Failed to sync budgets")
        }

        viewModel = HomeViewModel(
            getTransactionsUseCase,
            addTransactionUseCase,
            repository,
            networkMonitor,
            getBudgetsWithSpendingUseCase,
            budgetRepository
        )
        // Trigger initial sync which fails
        viewModel.refreshSync(force = true)
        advanceUntilIdle()

        // Verify the ViewModel has an error state initially
        assertEquals("Failed to sync transactions", viewModel.state.value.error)

        // 2. Change mocks so that the next sync succeeds
        whenever(repository.syncTransactions(25)).thenReturn(null)
        runBlocking {
            whenever(budgetRepository.syncBudgets()).thenReturn(null)
        }

        // 3. Simulating user clicking Retry
        viewModel.refreshSync(force = true)
        advanceUntilIdle()

        // Verify that retry is actually syncing (sync method called again)
        org.mockito.Mockito.verify(repository, times(2)).syncTransactions(25)
        runBlocking {
            org.mockito.Mockito.verify(budgetRepository, times(2)).syncBudgets()
        }

        // Verify the error is removed/cleared from the home screen state
        assertEquals(null, viewModel.state.value.error)
    }
}
