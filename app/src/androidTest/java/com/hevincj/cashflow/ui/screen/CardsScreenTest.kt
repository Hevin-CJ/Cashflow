package com.hevincj.cashflow.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavController
import com.hevincj.cashflow.domain.models.CreditCard
import com.hevincj.cashflow.ui.screen.state.CardsUiState
import com.hevincj.cashflow.ui.screen.viewmodel.CardsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CardsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = mock<CardsViewModel>()
    private val navController = mock<NavController>()
    private val stateFlow = MutableStateFlow(CardsUiState())

    @Test
    fun testCardsScreenDisplaysCards() {
        val sampleCards = listOf(
            CreditCard(
                id = "1",
                balance = 12500.0,
                cardNumber = "4111 2222 3333 4444",
                cardHolder = "Johnathan Doe",
                gradientColors = listOf(0xFF000000L, 0xFFFFFFFFL)
            )
        )
        stateFlow.value = CardsUiState(cards = sampleCards, isLoading = false)
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            CardsScreen(
                innerPadding = PaddingValues(),
                rootNavController = navController,
                viewModel = viewModel
            )
        }

        // Verify title
        composeTestRule.onNodeWithText("My Card").assertIsDisplayed()

        // Verify card details
        composeTestRule.onNodeWithText("$12,500.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("4111 2222 3333 4444").assertIsDisplayed()
        composeTestRule.onNodeWithText("JOHNATHAN DOE").assertIsDisplayed()
    }

    @Test
    fun testCardsScreenDisplaysEmptyState() {
        stateFlow.value = CardsUiState(cards = emptyList(), isLoading = false)
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            CardsScreen(
                innerPadding = PaddingValues(),
                rootNavController = navController,
                viewModel = viewModel
            )
        }

        // Verify empty state messages
        composeTestRule.onNodeWithText("No cards found").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add a credit or debit card to start tracking your balances.").assertIsDisplayed()
    }
}
