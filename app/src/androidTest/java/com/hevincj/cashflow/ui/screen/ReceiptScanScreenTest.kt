package com.hevincj.cashflow.ui.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.hevincj.cashflow.ui.screen.state.ScanUiState
import com.hevincj.cashflow.ui.screen.viewmodel.ScanViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ReceiptScanScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val scanViewModel = mock<ScanViewModel>()
    private val scanStateFlow = MutableStateFlow(ScanUiState())

    @org.junit.Before
    fun setUp() {
        whenever(scanViewModel.state).thenReturn(scanStateFlow)
    }

    @Test
    fun testReceiptScanScreenDisplaysPlaceholderAndButton() {
        composeTestRule.setContent {
            ReceiptScanScreen(
                onNavigateBack = {},
                onNavigateToAddTransaction = { _, _, _, _, _ -> },
                viewModel = scanViewModel
            )
        }

        // Verify title & buttons/placeholders are displayed
        composeTestRule.onNodeWithText("Receipt AI Scan").assertIsDisplayed()
        composeTestRule.onNodeWithText("No receipt image selected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Select from Gallery").assertIsDisplayed()
        composeTestRule.onNodeWithText("Testing Gemini API Key (Optional)").assertIsDisplayed()
    }
}
