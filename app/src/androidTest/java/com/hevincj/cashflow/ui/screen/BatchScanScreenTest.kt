package com.hevincj.cashflow.ui.screen

import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.rule.GrantPermissionRule
import com.hevincj.cashflow.domain.repository.ScanRepository
import com.hevincj.cashflow.domain.repository.TransactionRepository
import com.hevincj.cashflow.ui.screen.viewmodel.ScanViewModel
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class BatchScanScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private val scanRepository = mock<ScanRepository>()
    private val transactionRepository = mock<TransactionRepository>()
    private val scanViewModel = mock<ScanViewModel>()

    @org.junit.Before
    fun setUp() {
        whenever(scanViewModel.scanRepository).thenReturn(scanRepository)
        whenever(scanViewModel.transactionRepository).thenReturn(transactionRepository)
    }

    @Test
    fun testBatchScanScreenDisplaysCameraPermissionOrView() {
        composeTestRule.setContent {
            BatchScanScreen(
                onNavigateBack = {},
                viewModel = scanViewModel
            )
        }

        // Verify the main title is displayed
        composeTestRule.onNodeWithText("Batch Barcode Scanner").assertIsDisplayed()
    }
}
