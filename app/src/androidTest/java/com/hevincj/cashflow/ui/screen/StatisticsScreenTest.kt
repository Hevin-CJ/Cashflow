package com.hevincj.cashflow.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.hevincj.cashflow.domain.models.TransactionStats
import com.hevincj.cashflow.ui.screen.state.StatsUiState
import com.hevincj.cashflow.ui.screen.viewmodel.StatsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.YearMonth

class StatisticsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = mock<StatsViewModel>()
    private val stateFlow = MutableStateFlow(StatsUiState())

    @Test
    fun testStatisticsScreenDisplaysOverviewAndTabDetails() {
        val dummyStats = TransactionStats(
            totalIncome = 4500.0,
            totalExpenses = 1200.0,
            weeklyIncome = listOf(1000f, 1500f, 800f, 1200f),
            weeklyExpenses = listOf(300f, 400f, 200f, 300f),
            recentTransactions = emptyList()
        )
        val selectedMonth = YearMonth.of(2026, 6)
        stateFlow.value = StatsUiState(
            stats = dummyStats,
            selectedMonth = selectedMonth,
            availableMonths = listOf(selectedMonth),
            isLoading = false
        )
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            StatisticsScreen(innerPadding = PaddingValues(), viewModel = viewModel)
        }

        // Verify titles
        composeTestRule.onNodeWithText("Statistics").assertIsDisplayed()
        composeTestRule.onNodeWithText("Overview").assertIsDisplayed()

        // Verify Income/Expense totals displayed on the cards
        composeTestRule.onNodeWithText("$4500").assertIsDisplayed()
        composeTestRule.onNodeWithText("$1200").assertIsDisplayed()
    }
}
