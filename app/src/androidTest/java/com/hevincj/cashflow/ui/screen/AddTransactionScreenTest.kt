package com.hevincj.cashflow.ui.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.navigation.NavController
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.ui.screen.state.AddTransactionUiState
import com.hevincj.cashflow.ui.screen.viewmodel.AddTransactionViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AddTransactionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val navController = mock<NavController>()
    private val viewModel = mock<AddTransactionViewModel>()
    private val stateFlow = MutableStateFlow(AddTransactionUiState())

    @Test
    fun testAddTransactionScreenDisplaysElements() {
        stateFlow.value = AddTransactionUiState(
            amount = "",
            type = TransactionType.EXPENSE,
            category = TransactionCategory.GROCERIES,
            description = "",
            isLoading = false
        )
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            AddTransactionScreen(navController = navController, viewModel = viewModel)
        }

        // Verify key headers and fields are present
        composeTestRule.onNodeWithText("ADD NOTE (OPTIONAL)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save Transaction").assertIsDisplayed()
    }

    @Test
    fun testAddTransactionScreenInputsTriggerCallbacks() {
        stateFlow.value = AddTransactionUiState(
            amount = "",
            type = TransactionType.EXPENSE,
            category = TransactionCategory.GROCERIES,
            description = "",
            isLoading = false
        )
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            AddTransactionScreen(navController = navController, viewModel = viewModel)
        }

        // Fill in description
        composeTestRule.onNodeWithText("e.g. Weekly grocery or monthly income").performTextInput("Weekly groceries")
        verify(viewModel).onDescriptionChange("Weekly groceries")

        // Fill in amount (BasicTextField doesn't have simple text placeholder match, but we can search for the "0.00" label or use semantics)
        composeTestRule.onNodeWithText("0.00").performTextInput("150.0")
        verify(viewModel).onAmountChange("150.0")
    }

    @Test
    fun testAddTransactionScreenClickSaveTriggersViewModel() {
        stateFlow.value = AddTransactionUiState(
            amount = "150.0",
            type = TransactionType.EXPENSE,
            category = TransactionCategory.GROCERIES,
            description = "Groceries",
            isLoading = false
        )
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            AddTransactionScreen(navController = navController, viewModel = viewModel)
        }

        // Click Save Transaction CTA
        composeTestRule.onNodeWithText("Save Transaction").performClick()
        verify(viewModel).saveTransaction()
    }

    @Test
    fun testAddTransactionScreenDisplaysErrorMessage() {
        stateFlow.value = AddTransactionUiState(
            amount = "",
            type = TransactionType.EXPENSE,
            category = TransactionCategory.GROCERIES,
            description = "",
            isLoading = false,
            errorMessage = "Invalid amount entered"
        )
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            AddTransactionScreen(navController = navController, viewModel = viewModel)
        }

        // Verify error message banner is displayed
        composeTestRule.onNodeWithText("Invalid amount entered").assertIsDisplayed()
    }

    @Test
    fun testAddTransactionScreenSelectEntertainmentCategory() {
        stateFlow.value = AddTransactionUiState(
            amount = "",
            type = TransactionType.EXPENSE,
            category = TransactionCategory.GROCERIES,
            description = "",
            isLoading = false
        )
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            AddTransactionScreen(navController = navController, viewModel = viewModel)
        }

        // Scroll to "Entertainment" category as it starts off-screen in the horizontal scroll row
        composeTestRule.onNodeWithText("Entertainment")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        // Verify that viewmodel.onCategoryChange(TransactionCategory.ENTERTAINMENT) is called
        verify(viewModel).onCategoryChange(TransactionCategory.ENTERTAINMENT)
    }
}
