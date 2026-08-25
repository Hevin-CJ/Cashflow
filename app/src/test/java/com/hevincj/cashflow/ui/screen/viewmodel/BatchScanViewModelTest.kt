package com.hevincj.cashflow.ui.screen.viewmodel

import com.hevincj.cashflow.domain.models.ScanResult
import com.hevincj.cashflow.domain.repository.ScanRepository
import com.hevincj.cashflow.domain.repository.TransactionRepository
import com.hevincj.cashflow.domain.usecase.AnalyzeReceiptUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class BatchScanViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val scanRepository: ScanRepository = mock()
    private val transactionRepository: TransactionRepository = mock()
    private val analyzeReceiptUseCase: AnalyzeReceiptUseCase = mock()

    private lateinit var viewModel: ScanViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ScanViewModel(
            scanRepository = scanRepository,
            transactionRepository = transactionRepository,
            analyzeReceiptUseCase = analyzeReceiptUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testValidBarcodeIsAddedSuccessfully() = runTest {
        val barcode = "8901030383823"
        whenever(scanRepository.lookupBarcode(barcode)).thenReturn(
            ScanResult(barcode = barcode, productName = "Dove Soap", category = "Groceries", price = 45.0, currency = "INR")
        )

        viewModel.onBarcodeScanned(barcode)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.scannedCodes.size)
        assertEquals(barcode, state.scannedCodes.first())
        assertEquals("Dove Soap", state.scannedProducts[barcode]?.productName)
        assertEquals("45.00", state.commonAmountString)
    }

    @Test
    fun testShortEan8BarcodeIsAddedSuccessfully() = runTest {
        val ean8Barcode = "1234567"
        whenever(scanRepository.lookupBarcode(ean8Barcode)).thenReturn(
            ScanResult(barcode = ean8Barcode, productName = "Snack Bar", category = "Groceries", price = 20.0, currency = "INR")
        )

        viewModel.onBarcodeScanned(ean8Barcode)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.scannedCodes.size)
        assertEquals(ean8Barcode, state.scannedCodes.first())
    }

    @Test
    fun testUrlAndUpiStringsAreIgnored() = runTest {
        viewModel.onBarcodeScanned("https://example.com/item")
        viewModel.onBarcodeScanned("upi://pay?pa=test@upi&pn=Test")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.scannedCodes.isEmpty())
    }

    @Test
    fun testDeduplicationTogglePreventsDuplicatesWhenEnabled() = runTest {
        viewModel.onAddBarcodeOnceChanged(true)
        val barcode = "8901030383823"
        whenever(scanRepository.lookupBarcode(barcode)).thenReturn(
            ScanResult(barcode = barcode, productName = "Dove Soap", category = "Groceries", price = 45.0, currency = "INR")
        )

        viewModel.onBarcodeScanned(barcode)
        advanceUntilIdle()

        // Wait past 1 second debounce
        Thread.sleep(1050)

        viewModel.onBarcodeScanned(barcode)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.scannedCodes.size)
    }

    @Test
    fun testDuplicateAllowedWhenAddBarcodeOnceIsDisabled() = runTest {
        viewModel.onAddBarcodeOnceChanged(false)
        val barcode = "8901030383823"
        whenever(scanRepository.lookupBarcode(barcode)).thenReturn(
            ScanResult(barcode = barcode, productName = "Dove Soap", category = "Groceries", price = 45.0, currency = "INR")
        )

        viewModel.onBarcodeScanned(barcode)
        advanceUntilIdle()

        // Wait past 1 second debounce
        Thread.sleep(1050)

        viewModel.onBarcodeScanned(barcode)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.scannedCodes.size)
    }

    @Test
    fun testClearAllResetsScannedCodesAndAmount() = runTest {
        val barcode = "8901030383823"
        viewModel.onBarcodeScanned(barcode)
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.scannedCodes.size)

        viewModel.onClearAll()
        val state = viewModel.state.value
        assertTrue(state.scannedCodes.isEmpty())
        assertEquals("", state.commonAmountString)
    }

    @Test
    fun testEditingProductNameUpdatesState() = runTest {
        val barcode = "8901030383823"
        viewModel.onBarcodeScanned(barcode)
        advanceUntilIdle()

        viewModel.onStartEditingProduct(barcode, "Custom Dove Bar")
        viewModel.onEditingNameChanged("Custom Dove Soap 100g")
        viewModel.onSaveEditedProduct()

        val state = viewModel.state.value
        assertEquals("Custom Dove Soap 100g", state.scannedProducts[barcode]?.productName)
    }
}
