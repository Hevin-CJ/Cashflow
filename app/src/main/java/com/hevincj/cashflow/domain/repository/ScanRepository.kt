package com.hevincj.cashflow.domain.repository

import com.hevincj.cashflow.domain.models.ReceiptAnalysisOutcome
import com.hevincj.cashflow.domain.models.ReceiptScanResult
import com.hevincj.cashflow.domain.models.ScanResult
import com.hevincj.cashflow.domain.models.UpiQrResult

interface ScanRepository {
    suspend fun scanSingleBarcode(): String?
    suspend fun lookupBarcode(barcode: String): ScanResult?
    suspend fun lookupBatchBarcodes(barcodes: List<String>): List<ScanResult>
    suspend fun analyzeReceipt(imageBytes: ByteArray): ReceiptScanResult?
    suspend fun analyzeReceiptWithOutcome(imageBytes: ByteArray): ReceiptAnalysisOutcome
    suspend fun generateUpiQr(amount: Double?, note: String?): UpiQrResult?
}
