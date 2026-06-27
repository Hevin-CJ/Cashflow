package com.hevincj.cashflow.ui.screen

import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.rule.GrantPermissionRule
import com.hevincj.cashflow.ui.screen.state.ScanUiState
import com.hevincj.cashflow.ui.screen.viewmodel.ScanEvent
import com.hevincj.cashflow.ui.screen.viewmodel.ScanViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class BatchScanScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private val scanViewModel = mock<ScanViewModel>()
    private val scanStateFlow = MutableStateFlow(ScanUiState())
    private val eventFlow = MutableSharedFlow<ScanEvent>()

    @org.junit.Before
    fun setUp() {
        whenever(scanViewModel.state).thenReturn(scanStateFlow)
        whenever(scanViewModel.eventFlow).thenReturn(eventFlow)
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
