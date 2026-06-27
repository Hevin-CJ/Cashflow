package com.hevincj.cashflow.ui.screen.state

import androidx.compose.runtime.Immutable
import com.hevincj.cashflow.domain.models.ScanResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

@Immutable
data class ScanUiState(
    val scannedCodes: ImmutableList<String> = persistentListOf(),
    val scannedProducts: ImmutableMap<String, ScanResult> = persistentMapOf(),
    val resolvingCodes: ImmutableList<String> = persistentListOf(),
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
