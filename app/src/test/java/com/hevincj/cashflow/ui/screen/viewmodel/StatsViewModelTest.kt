package com.hevincj.cashflow.ui.screen.viewmodel

import com.hevincj.cashflow.MainDispatcherRule
import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.models.TransactionStats
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.domain.repository.TransactionRepository
import com.hevincj.cashflow.domain.usecase.GetStatisticsUseCase
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ShoppingBag
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    lateinit var repository: TransactionRepository

    @Mock
    lateinit var getStatisticsUseCase: GetStatisticsUseCase

    private lateinit var viewModel: StatsViewModel

    private val sampleTransactions = listOf(
        Transaction(
            id = "1",
            title = "Groceries",
            timestamp = 1718278312000L, // June 13 2026
            amount = -900.0,
            icon = androidx.compose.material.icons.Icons.Rounded.ShoppingBag,
            iconBgColor = Color(0xFFF19E79),
            type = TransactionType.EXPENSE,
            category = TransactionCategory.GROCERIES,
            description = "Week food shopping",
            isSynced = true
        )
    )

    private val sampleStats = TransactionStats(
        totalIncome = 0.0,
        totalExpenses = 900.0,
        weeklyIncome = listOf(0f, 0f, 0f, 0f),
        weeklyExpenses = listOf(0f, 900f, 0f, 0f),
        recentTransactions = sampleTransactions
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(repository.getAllTransactions()).thenReturn(flowOf(sampleTransactions))
        whenever(getStatisticsUseCase.invoke(any(), any())).thenReturn(flowOf(sampleStats))
        
        viewModel = StatsViewModel(repository, getStatisticsUseCase)
    }

    @Test
    fun testInitializationLoadsAvailableMonthsAndFetchesStats() = runTest {
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(sampleStats, state.stats)
        
        // Available months should contain YearMonth of transaction and YearMonth.now()
        val expectedMonth = YearMonth.of(2026, 6)
        assertTrue(state.availableMonths.contains(expectedMonth))
    }

    @Test
    fun testSelectMonthUpdatesStateAndRefetchesStats() = runTest {
        advanceUntilIdle()

        val newMonth = YearMonth.of(2026, 5)
        viewModel.selectMonth(newMonth)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(newMonth, state.selectedMonth)
    }

    // Helper function to bypass standard kotlin collection checks
    private fun <T> Collection<T>.contains(element: T): Boolean {
        return this.toSet().contains(element)
    }
}
