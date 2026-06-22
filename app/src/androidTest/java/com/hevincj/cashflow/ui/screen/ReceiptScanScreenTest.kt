package com.hevincj.cashflow.ui.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.hevincj.cashflow.domain.repository.ScanRepository
import com.hevincj.cashflow.ui.screen.viewmodel.ScanViewModel
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ReceiptScanScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val scanRepository = mock<ScanRepository>()
    private val scanViewModel = mock<ScanViewModel>()

    @org.junit.Before
    fun setUp() {
        whenever(scanViewModel.scanRepository).thenReturn(scanRepository)
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
