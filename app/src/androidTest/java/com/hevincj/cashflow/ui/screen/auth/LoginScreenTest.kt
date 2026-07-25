package com.hevincj.cashflow.ui.screen.auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.navigation.NavController
import com.hevincj.cashflow.ui.screen.state.AuthUiState
import com.hevincj.cashflow.ui.screen.state.AuthState
import com.hevincj.cashflow.ui.screen.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val navController = mock<NavController>()
    private val viewModel = mock<AuthViewModel>()
    private val stateFlow = MutableStateFlow(AuthUiState())

    @Test
    fun testLoginScreenDisplaysFieldsAndButtons() {
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            LoginScreen(navController = navController, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Welcome Back").assertIsDisplayed()
        composeTestRule.onNodeWithText("Username").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Login").assertIsDisplayed()
    }

    @Test
    fun testLoginScreenInputsTriggerViewModelCallbacks() {
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            LoginScreen(navController = navController, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Username").performTextInput("test_username")
        verify(viewModel).onUsernameChange("test_username")

        composeTestRule.onNodeWithText("Password").performTextInput("test_password")
        verify(viewModel).onPasswordChange("test_password")
    }

    @Test
    fun testLoginButtonClickTriggersLogin() {
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            LoginScreen(navController = navController, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Login").performClick()
        verify(viewModel).initiateLogin()
    }

    @Test
    fun testLoginScreenDisablesButtonOnLoading() {
        stateFlow.value = AuthUiState(
            username = "test",
            password = "pwd",
            authState = AuthState.Loading
        )
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            LoginScreen(navController = navController, viewModel = viewModel)
        }

        // Verify that the login text disappears and the indeterminate progress indicator is shown
        composeTestRule.onNodeWithText("Login").assertDoesNotExist()
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }
}
