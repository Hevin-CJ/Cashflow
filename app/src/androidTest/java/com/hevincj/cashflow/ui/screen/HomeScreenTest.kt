package com.hevincj.cashflow.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavController
import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.ui.screen.state.BalanceRange
import com.hevincj.cashflow.ui.screen.state.HomeUiState
import com.hevincj.cashflow.ui.screen.viewmodel.HomeViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

import com.hevincj.cashflow.ui.screen.viewmodel.ScanViewModel

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val navController = mock<NavController>()
    private val viewModel = mock<HomeViewModel>()
    private val scanViewModel = mock<ScanViewModel>()
    private val stateFlow = MutableStateFlow(HomeUiState())

    private val sampleTransactions = listOf(
        Transaction(
            id = "1",
            title = "Salary",
            timestamp = System.currentTimeMillis(),
            amount = 5000.00,
            icon = TransactionCategory.SALARY.icon,
            iconBgColor = TransactionCategory.SALARY.iconBgColor,
            type = TransactionType.INCOME,
            category = TransactionCategory.SALARY,
            description = "Monthly pay",
            isSynced = true
        )
    )

    @Test
    fun testHomeScreenDisplaysDashboardElements() {
        val state = HomeUiState(
            transactions = sampleTransactions.toImmutableList(),
            totalBalance = 5000.0,
            totalIncome = 5000.0,
            totalExpense = 0.0,
            balanceRange = BalanceRange.ALL_TIME,
            isLoading = false
        )
        stateFlow.value = state
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            HomeScreen(
                innerPadding = PaddingValues(),
                rootNavController = navController,
                viewModel = viewModel,
                scanViewModel = scanViewModel
            )
        }

        // Verify balance display
        composeTestRule.onNodeWithText("Total Balance").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("$5,000.00").onFirst().assertIsDisplayed()

        // Verify income/expense card details
        composeTestRule.onNodeWithText("Income").assertIsDisplayed()
        composeTestRule.onNodeWithText("Expense").assertIsDisplayed()

        // Verify transactions title & transaction item list
        composeTestRule.onNodeWithText("Transactions").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Salary").onFirst().assertIsDisplayed()
        composeTestRule.onNodeWithText("+$5000").assertIsDisplayed()
    }

    @Test
    fun testHomeScreenDropdownChangesSelectedRange() {
        val state = HomeUiState(
            transactions = persistentListOf(),
            totalBalance = 0.0,
            balanceRange = BalanceRange.ALL_TIME,
            isLoading = false
        )
        stateFlow.value = state
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            HomeScreen(
                innerPadding = PaddingValues(),
                rootNavController = navController,
                viewModel = viewModel,
                scanViewModel = scanViewModel
            )
        }

        // Open dropdown menu
        composeTestRule.onNodeWithText("Total Balance").performClick()
        
        // Select 'This Month' option
        composeTestRule.onNodeWithText("This Month").performClick()
        
        // Verify viewmodel callback is triggered
        verify(viewModel).onBalanceRangeChange(BalanceRange.THIS_MONTH)
    }

    @Test
    fun testHomeScreenDisplaysShimmerLoadingState() {
        val state = HomeUiState(
            transactions = persistentListOf(),
            totalBalance = 0.0,
            isLoading = true,
            error = null
        )
        stateFlow.value = state
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            HomeScreen(
                innerPadding = PaddingValues(),
                rootNavController = navController,
                viewModel = viewModel,
                scanViewModel = scanViewModel
            )
        }

        // Shimmer elements are rendering, verify default transaction details are not shown
        composeTestRule.onNodeWithText("Salary").assertDoesNotExist()
        composeTestRule.onNodeWithText("Transactions").assertDoesNotExist()
        // But the Top Bar title "Home" is displayed
        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
    }

    @Test
    fun testHomeScreenDisplaysEmptyState() {
        val state = HomeUiState(
            transactions = persistentListOf(),
            totalBalance = 0.0,
            isLoading = false,
            error = null
        )
        stateFlow.value = state
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            HomeScreen(
                innerPadding = PaddingValues(),
                rootNavController = navController,
                viewModel = viewModel,
                scanViewModel = scanViewModel
            )
        }

        // Verify empty state warning elements
        composeTestRule.onNodeWithText("No transactions found").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your transaction history will appear here once you make your first payment.").assertIsDisplayed()
    }

    @Test
    fun testHomeScreenDisplaysErrorBanner() {
        val state = HomeUiState(
            transactions = persistentListOf(),
            totalBalance = 0.0,
            isLoading = false,
            error = "Database read timeout error"
        )
        stateFlow.value = state
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            HomeScreen(
                innerPadding = PaddingValues(),
                rootNavController = navController,
                viewModel = viewModel,
                scanViewModel = scanViewModel
            )
        }

        // Verify error banner layout is shown
        composeTestRule.onNodeWithText("Sync Failure").assertIsDisplayed()
        composeTestRule.onNodeWithText("Database read timeout error").assertIsDisplayed()
    }
}
