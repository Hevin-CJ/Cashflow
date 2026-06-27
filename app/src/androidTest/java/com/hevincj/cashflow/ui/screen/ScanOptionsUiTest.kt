package com.hevincj.cashflow.ui.screen

import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavController
import androidx.test.rule.GrantPermissionRule
import com.hevincj.cashflow.ui.screen.state.ScanUiState
import com.hevincj.cashflow.ui.screen.viewmodel.ScanViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ScanOptionsUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private val navController = mock<NavController>()
    private val scanViewModel = mock<ScanViewModel>()
    private val scanStateFlow = MutableStateFlow(ScanUiState())

    @org.junit.Before
    fun setUp() {
        whenever(scanViewModel.state).thenReturn(scanStateFlow)
    }

    @Test
    fun testScanOptionsUiDisplaysTabsAndButtons() {
        composeTestRule.setContent {
            ScanOptionsUi(
                onDismissRequest = {},
                onBatchBarcodeClick = {},
                onReceiptScanClick = {},
                onUpiQrClick = {},
                rootNavController = navController,
                viewModel = scanViewModel
            )
        }

        // Verify tabs are displayed
        composeTestRule.onNodeWithText("Receipts").assertIsDisplayed()
        composeTestRule.onNodeWithText("QR code").assertIsDisplayed()

        // Verify bottom options/actions are displayed
        composeTestRule.onNodeWithText("Receipt AI scan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bulk adding").assertIsDisplayed()
    }
}
