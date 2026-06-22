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

class RegisterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val navController = mock<NavController>()
    private val viewModel = mock<AuthViewModel>()
    private val stateFlow = MutableStateFlow(AuthUiState())

    @Test
    fun testRegisterScreenDisplaysFieldsAndButtons() {
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            RegisterScreen(navController = navController, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Create Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Username").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Register").assertIsDisplayed()
    }

    @Test
    fun testRegisterScreenInputsTriggerViewModelCallbacks() {
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            RegisterScreen(navController = navController, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Username").performTextInput("test_register_user")
        verify(viewModel).onUsernameChange("test_register_user")

        composeTestRule.onNodeWithText("Password").performTextInput("test_register_pass")
        verify(viewModel).onPasswordChange("test_register_pass")
    }

    @Test
    fun testRegisterButtonClickTriggersRegister() {
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            RegisterScreen(navController = navController, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Register").performClick()
        verify(viewModel).register()
    }

    @Test
    fun testRegisterScreenDisablesButtonOnLoading() {
        stateFlow.value = AuthUiState(
            username = "test",
            password = "pwd",
            authState = AuthState.Loading
        )
        whenever(viewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            RegisterScreen(navController = navController, viewModel = viewModel)
        }

        // Verify that the register text disappears and the indeterminate progress indicator is shown
        composeTestRule.onNodeWithText("Register").assertDoesNotExist()
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }
}
