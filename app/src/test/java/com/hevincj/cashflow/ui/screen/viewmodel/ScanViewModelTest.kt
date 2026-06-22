package com.hevincj.cashflow.ui.screen.viewmodel

import com.hevincj.cashflow.MainDispatcherRule
import com.hevincj.cashflow.domain.models.ScanResult
import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.repository.ScanRepository
import com.hevincj.cashflow.domain.repository.TransactionRepository
import com.hevincj.cashflow.utils.isProductValid
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    lateinit var scanRepository: ScanRepository

    @Mock
    lateinit var transactionRepository: TransactionRepository

    private lateinit var viewModel: ScanViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = ScanViewModel(scanRepository, transactionRepository)
    }

    private fun scanBarcode(barcode: String) {
        viewModel.onBarcodeScanned(barcode)
        viewModel.onBarcodeScanned(barcode)
        viewModel.onBarcodeScanned(barcode)
    }

    @Test
    fun testBarcodeScannedIgnoresShortBarcode() = runTest {
        scanBarcode("12345678901") // Too short (< 12)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.scannedCodes.isEmpty())
    }

    @Test
    fun testBarcodeScannedIgnoresNonDigitBarcode() = runTest {
        scanBarcode("89012345678a") // Contains letter but length 12
        advanceUntilIdle()

        assertTrue(viewModel.state.value.scannedCodes.isEmpty())
    }

    @Test
    fun testBarcodeScannedAcceptsValidBarcode() = runTest {
        val barcode = "8901030356502"
        whenever(scanRepository.lookupBarcode(barcode)).thenReturn(
            ScanResult(barcode, "Organic Milk", "Groceries", 120.0, "INR")
        )

        scanBarcode(barcode)
        advanceUntilIdle()

        assertEquals(listOf(barcode), viewModel.state.value.scannedCodes)
        assertEquals("Organic Milk", viewModel.state.value.scannedProducts[barcode]?.productName)
    }

    @Test
    fun testBarcodeScannedIgnoresRepeatedScansWhenAddOnceTrue() = runTest {
        val barcode = "8901030356502"
        whenever(scanRepository.lookupBarcode(barcode)).thenReturn(
            ScanResult(barcode, "Organic Milk", "Groceries", 120.0, "INR")
        )

        viewModel.onAddBarcodeOnceChanged(true)
        scanBarcode(barcode)
        advanceUntilIdle()

        Thread.sleep(2005)

        // Scan the same barcode again - should be ignored (not toggle-deleted)
        scanBarcode(barcode)
        advanceUntilIdle()

        assertEquals(listOf(barcode), viewModel.state.value.scannedCodes)
    }

    @Test
    fun testBarcodeScannedAllowsRepeatedScansWhenAddOnceFalse() = runTest {
        val barcode = "8901030356502"
        whenever(scanRepository.lookupBarcode(barcode)).thenReturn(
            ScanResult(barcode, "Organic Milk", "Groceries", 120.0, "INR")
        )

        viewModel.onAddBarcodeOnceChanged(false)
        scanBarcode(barcode)
        advanceUntilIdle()

        Thread.sleep(2005)

        scanBarcode(barcode)
        advanceUntilIdle()

        assertEquals(listOf(barcode, barcode), viewModel.state.value.scannedCodes)
    }

    @Test
    fun testProductValidationConstraints() {
        val barcode = "8901030356502"

        // Invalid placeholders
        assertFalse(isProductValid(null, barcode))
        assertFalse(isProductValid("", barcode))
        assertFalse(isProductValid("Barcode Item", barcode))
        assertFalse(isProductValid("Unknown Product", barcode))
        assertFalse(isProductValid("Barcode Item 8901030356502", barcode))
        assertFalse(isProductValid("Unknown Product: 8901030356502", barcode))
        assertFalse(isProductValid(barcode, barcode))

        // Valid product names
        assertTrue(isProductValid("Water Bottle 8901030356502", barcode))
        assertTrue(isProductValid("Organic Milk", barcode))
        assertTrue(isProductValid("Coca Cola", barcode))
    }

    @Test
    fun testSaveBatchTransactionUsesEnteredTitle() = runTest {
        val barcode = "8901030356502"
        whenever(scanRepository.lookupBarcode(barcode)).thenReturn(
            ScanResult(barcode, "Organic Milk", "Groceries", 120.0, "INR")
        )

        scanBarcode(barcode)
        advanceUntilIdle()

        viewModel.onTitleChanged("My Custom Shopping Note")
        viewModel.saveBatchTransaction()
        advanceUntilIdle()

        val captor = argumentCaptor<Transaction>()
        verify(transactionRepository).insertTransaction(captor.capture())

        assertEquals("My Custom Shopping Note", captor.firstValue.title)
    }

    @Test
    fun testSaveBatchTransactionFallsBackToProductNames() = runTest {
        val code1 = "8901030356501"
        val code2 = "8901030356502"

        whenever(scanRepository.lookupBarcode(code1)).thenReturn(ScanResult(code1, "Pepsi", "Groceries", 40.0, "INR"))
        whenever(scanRepository.lookupBarcode(code2)).thenReturn(ScanResult(code2, "Chips", "Groceries", 20.0, "INR"))

        scanBarcode(code1)
        advanceUntilIdle()

        Thread.sleep(1505)

        scanBarcode(code2)
        advanceUntilIdle()

        viewModel.saveBatchTransaction()
        advanceUntilIdle()

        val captor = argumentCaptor<Transaction>()
        verify(transactionRepository).insertTransaction(captor.capture())

        assertEquals("Pepsi, Chips", captor.firstValue.title)
    }
}
