package com.hevincj.cashflow.ui.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavController
import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.ui.screen.state.HomeUiState
import com.hevincj.cashflow.ui.screen.viewmodel.HomeViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AllTransactionsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val navController = mock<NavController>()
    private val viewModel = mock<HomeViewModel>()
    private val stateFlow = MutableStateFlow(HomeUiState())

    private val transactions = listOf(
        Transaction(
            id = "1",
            title = "Groceries",
            timestamp = System.currentTimeMillis(),
            amount = -50.0,
            icon = TransactionCategory.GROCERIES.icon,
            iconBgColor = TransactionCategory.GROCERIES.iconBgColor,
            type = TransactionType.EXPENSE,
            category = TransactionCategory.GROCERIES,
            description = "weekly groceries",
            isSynced = true
        ),
        Transaction(
            id = "2",
            title = "Salary",
            timestamp = System.currentTimeMillis(),
            amount = 3000.0,
            icon = TransactionCategory.SALARY.icon,
            iconBgColor = TransactionCategory.SALARY.iconBgColor,
            type = TransactionType.INCOME,
            category = TransactionCategory.SALARY,
            description = "stipend",
            isSynced = true
        )
    )

    @Test
    fun testAllTransactionsDisplaysListAndFiltersBySearchQuery() {
        val state = HomeUiState(
            transactions = transactions.toImmutableList(),
            isLoading = false
        )
        stateFlow.value = state
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            AllTransactionsScreen(navController = navController, viewModel = viewModel)
        }

        // Verify all transactions are initially displayed by checking amounts
        composeTestRule.onNodeWithText("-$50").assertIsDisplayed()
        composeTestRule.onNodeWithText("+$3000").assertIsDisplayed()

        // Type search query "Sal"
        composeTestRule.onNodeWithText("Search transactions...").performTextInput("Sal")

        // Verify Groceries amount is filtered out, but Salary is kept
        composeTestRule.onNodeWithText("+$3000").assertIsDisplayed()
        composeTestRule.onNodeWithText("-$50").assertDoesNotExist()
    }

    @Test
    fun testAllTransactionsFiltersByCategoryPill() {
        val state = HomeUiState(
            transactions = transactions.toImmutableList(),
            isLoading = false
        )
        stateFlow.value = state
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            AllTransactionsScreen(navController = navController, viewModel = viewModel)
        }

        // Click "Salary" category pill (first matching node)
        composeTestRule.onAllNodesWithText("Salary").onFirst().performClick()

        // Salary transaction should be displayed, Groceries should not
        composeTestRule.onNodeWithText("+$3000").assertIsDisplayed()
        composeTestRule.onNodeWithText("-$50").assertDoesNotExist()
    }
}
