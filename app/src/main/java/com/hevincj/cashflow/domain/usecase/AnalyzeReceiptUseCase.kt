package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.models.ReceiptAnalysisOutcome
import com.hevincj.cashflow.domain.models.ReceiptScanResult
import com.hevincj.cashflow.domain.repository.ScanRepository
import javax.inject.Inject

class AnalyzeReceiptUseCase @Inject constructor(
    private val repository: ScanRepository
) {
    suspend operator fun invoke(imageBytes: ByteArray): ReceiptScanResult? {
        return repository.analyzeReceipt(imageBytes)
    }

    suspend fun analyze(imageBytes: ByteArray): ReceiptAnalysisOutcome {
        return repository.analyzeReceiptWithOutcome(imageBytes)
    }
}
