package com.hevincj.cashflow.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.navigation.NavController
import com.hevincj.cashflow.ui.screen.state.ProfileUiState
import com.hevincj.cashflow.ui.screen.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val navController = mock<NavController>()
    private val viewModel = mock<ProfileViewModel>()
    private val stateFlow = MutableStateFlow(ProfileUiState())

    @Test
    fun testProfileScreenDisplaysMenuItemsAndOpensLogoutDialog() {
        stateFlow.value = ProfileUiState(isLoggedOut = false)
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            ProfileScreen(
                rootNavController = navController,
                innerPaddingValues = PaddingValues(),
                viewModel = viewModel
            )
        }

        // Verify profile details and items are displayed
        composeTestRule.onNodeWithText("Account Info").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Logout").assertIsDisplayed()

        // Click on Logout menu item to show dialog
        composeTestRule.onNodeWithText("Logout").performClick()

        // Verify logout dialog is showing
        composeTestRule.onNodeWithText("Are you sure you want to log out?").assertIsDisplayed()

        // Click on Confirm inside the dialog
        composeTestRule.onNodeWithText("Confirm").performClick()

        // Verify viewModel.logout() is triggered
        verify(viewModel).logout()
    }
}
