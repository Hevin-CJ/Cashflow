package com.hevincj.cashflow.ui.screen.state

import com.hevincj.cashflow.domain.models.ScanResult

data class ScanUiState(
    val scannedCodes: List<String> = emptyList(),
    val scannedProducts: Map<String, ScanResult> = emptyMap(),
    val resolvingCodes: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val commonTitle: String = "",
    val commonAmountString: String = "",
    val isAmountEditedByUser: Boolean = false,
    val addBarcodeOnce: Boolean = true,
    val editingCode: String? = null,
    val editingName: String = "",
    val isAnalyzing: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false
)
