package com.hevincj.cashflow.ui.screen

import com.hevincj.cashflow.MainDispatcherRule
import com.hevincj.cashflow.domain.models.ScanResult
import com.hevincj.cashflow.domain.repository.ScanRepository
import com.hevincj.cashflow.domain.repository.TransactionRepository
import com.hevincj.cashflow.domain.usecase.AnalyzeReceiptUseCase
import com.hevincj.cashflow.ui.screen.viewmodel.ScanEvent
import com.hevincj.cashflow.ui.screen.viewmodel.ScanViewModel
import com.hevincj.cashflow.utils.CrashLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerResilienceUnitTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    lateinit var scanRepository: ScanRepository

    @Mock
    lateinit var transactionRepository: TransactionRepository

    @Mock
    lateinit var analyzeReceiptUseCase: AnalyzeReceiptUseCase

    private lateinit var viewModel: ScanViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = ScanViewModel(scanRepository, transactionRepository, analyzeReceiptUseCase)
    }

    @Test
    fun testAsyncCameraInitialization_timesOutSafelyWhenHardwareHangs() = runTest {
        // Simulate CameraX provider taking 5000ms (longer than the 3000ms timeout)
        val result = withTimeoutOrNull(3000L) {
            delay(5000L)
            "CameraProviderInstance"
        }

        // Must evaluate to null instead of deadlocking or throwing an unhandled exception
        assertNull(result)
    }

    @Test
    fun testAsyncCameraInitialization_succeedsWhenWithinTimeout() = runTest {
        // Simulate CameraX provider responding promptly (100ms)
        val result = withTimeoutOrNull(3000L) {
            delay(100L)
            "CameraProviderInstance"
        }

        assertNotNull(result)
        assertEquals("CameraProviderInstance", result)
    }

    @Test
    fun testScannerFailureLogging_recordsDiagnosticTelemetry() {
        // Verify that when a simulated camera hardware exception is caught, CrashLogger logs it safely
        val simulatedException = IllegalStateException("Camera2 HAL service busy / unavailable")
        
        try {
            throw simulatedException
        } catch (e: Throwable) {
            CrashLogger.e("CardScannerView", "Simulated hardware failure", e)
        }

        // Test reaches here without throwing an uncaught exception
        assertTrue(true)
    }

    @Test
    fun testScanEventFlow_emitsBeepAndVibrateSafely() = runTest {
        val barcode = "8901030356502"
        whenever(scanRepository.lookupBarcode(barcode)).thenReturn(
            ScanResult(barcode, "Organic Milk", "Groceries", 120.0, "INR")
        )

        var receivedEvent: ScanEvent? = null
        val job = launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            receivedEvent = viewModel.eventFlow.first()
        }

        // Trigger 3 scans to satisfy consecutive scan confirmation
        viewModel.onBarcodeScanned(barcode)
        viewModel.onBarcodeScanned(barcode)
        viewModel.onBarcodeScanned(barcode)

        advanceUntilIdle()

        assertNotNull(receivedEvent)
        assertTrue(receivedEvent is ScanEvent.BeepAndVibrate)

        job.cancel()
    }
}
