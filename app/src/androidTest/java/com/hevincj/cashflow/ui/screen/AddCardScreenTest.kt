package com.hevincj.cashflow.ui.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavController
import com.hevincj.cashflow.ui.screen.viewmodel.CardsViewModel
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class AddCardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = mock<CardsViewModel>()
    private val navController = mock<NavController>()

    @Test
    fun testAddCardScreenDisplaysFields() {
        composeTestRule.setContent {
            AddCardScreen(navController = navController, viewModel = viewModel)
        }

        // Verify the screen title (header) is displayed
        composeTestRule.onNode(hasText("Add Card") and !hasClickAction()).assertIsDisplayed()
        // Verify the button is displayed
        composeTestRule.onNode(hasText("Add Card") and hasClickAction()).assertIsDisplayed()
        composeTestRule.onNodeWithText("Card Holder Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Card Number").assertIsDisplayed()
        composeTestRule.onNodeWithText("Initial Balance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Select Card Theme").assertIsDisplayed()
    }

    @Test
    fun testAddCardScreenSubmitsValidData() {
        composeTestRule.setContent {
            AddCardScreen(navController = navController, viewModel = viewModel)
        }

        // Type info
        composeTestRule.onNodeWithText("Card Holder Name").performTextInput("Robert Downey")
        // Use a valid Visa card number that passes Luhn check: 4111 1111 1111 1111
        composeTestRule.onNodeWithText("Card Number").performTextInput("4111111111111111")
        composeTestRule.onNodeWithText("Initial Balance").performTextInput("1500.0")

        // Click Add Card Button
        composeTestRule.onNode(hasText("Add Card") and hasClickAction()).performClick()

        // Verify addCard callback was triggered on viewModel
        verify(viewModel).addCard(
            cardHolder = "Robert Downey",
            cardNumber = "4111 1111 1111 1111",
            balance = 1500.0,
            gradientColors = listOf(0xFF67E2AEL, 0xFFE8679AL, 0xFFF19E79L)
        )
        
        // Verify it navigated back
        verify(navController).popBackStack()
    }
}
