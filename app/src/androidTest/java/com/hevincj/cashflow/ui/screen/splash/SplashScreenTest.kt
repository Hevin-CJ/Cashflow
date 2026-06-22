package com.hevincj.cashflow.ui.screen.splash

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertIsDisplayed
import androidx.navigation.NavController
import com.hevincj.cashflow.ui.screen.viewmodel.SplashViewModel
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SplashScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val navController = mock<NavController>()
    private val viewModel = mock<SplashViewModel>()

    @Test
    fun testSplashScreenDisplaysLogo() {
        whenever(viewModel.isUserLoggedIn()).thenReturn(false)

        composeTestRule.setContent {
            SplashScreen(navController = navController, viewModel = viewModel)
        }

        composeTestRule.onNodeWithContentDescription("Logo").assertIsDisplayed()
    }
}
