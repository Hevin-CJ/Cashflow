package com.hevincj.cashflow.ui.screen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hevincj.cashflow.domain.models.ReceiptScanResult
import com.hevincj.cashflow.domain.models.ScanResult
import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.models.UpiQrResult
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.domain.repository.ScanRepository
import com.hevincj.cashflow.domain.repository.TransactionRepository
import com.hevincj.cashflow.domain.usecase.AnalyzeReceiptUseCase
import com.hevincj.cashflow.ui.screen.state.ScanUiState
import com.hevincj.cashflow.utils.isProductValid
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID
import javax.inject.Inject

sealed interface ScanEvent {
    object BeepAndVibrate : ScanEvent
    object Vibrate : ScanEvent
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scanRepository: ScanRepository,
    private val transactionRepository: TransactionRepository,
    private val analyzeReceiptUseCase: AnalyzeReceiptUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ScanEvent>(extraBufferCapacity = 64)
    val eventFlow: SharedFlow<ScanEvent> = _eventFlow.asSharedFlow()

    private val lastScannedMap = mutableMapOf<String, Long>()

    fun onBarcodeScanned(barcode: String) {
        if (barcode.isBlank() || barcode.length < 7 || barcode.length > 64) return
        val isUrl = barcode.startsWith("http://", ignoreCase = true) ||
                barcode.startsWith("https://", ignoreCase = true) ||
                barcode.startsWith("www.", ignoreCase = true) ||
                barcode.contains("://", ignoreCase = true) ||
                barcode.startsWith("upi://", ignoreCase = true)
        if (isUrl) return

        val currentTime = System.currentTimeMillis()
        val lastTime = lastScannedMap[barcode] ?: 0L
        if (currentTime - lastTime < 1000L) return

        lastScannedMap[barcode] = currentTime

        val currentState = _state.value
        if (currentState.addBarcodeOnce && currentState.scannedCodes.contains(barcode)) {
            return
        }

        _state.update { state ->
            val updatedCodes = state.scannedCodes.toMutableList().apply { add(barcode) }
            val updatedResolving = state.resolvingCodes.toMutableList().apply { add(barcode) }
            state.copy(
                scannedCodes = updatedCodes.toImmutableList(),
                resolvingCodes = updatedResolving.toImmutableList()
            )
        }
        _eventFlow.tryEmit(ScanEvent.BeepAndVibrate)
        updateCalculatedSum()

        // Real-time lookup
        viewModelScope.launch {
            try {
                val result = scanRepository.lookupBarcode(barcode)
                if (result != null) {
                    _state.update { state ->
                        val updatedProducts = state.scannedProducts.toMutableMap().apply {
                            put(barcode, result)
                        }
                        state.copy(scannedProducts = updatedProducts.toImmutableMap())
                    }
                }
            } catch (_: Exception) {
                // Safe fallback
            } finally {
                _state.update { state ->
                    val finalResolving = state.resolvingCodes.toMutableList().apply { remove(barcode) }
                    state.copy(resolvingCodes = finalResolving.toImmutableList())
                }
                updateCalculatedSum()
            }
        }
    }

    fun onAmountChanged(amount: String) {
        _state.update { state ->
            state.copy(
                commonAmountString = amount,
                isAmountEditedByUser = true
            )
        }
    }

    fun onTitleChanged(title: String) {
        _state.update { state ->
            state.copy(commonTitle = title)
        }
    }

    fun onAddBarcodeOnceChanged(enabled: Boolean) {
        _state.update { state ->
            state.copy(addBarcodeOnce = enabled)
        }
    }

    fun onClearAll() {
        _state.update { state ->
            state.copy(
                scannedCodes = persistentListOf(),
                commonAmountString = "",
                isAmountEditedByUser = false
            )
        }
    }

    fun onRemoveBarcode(barcode: String) {
        _state.update { state ->
            val updatedCodes = state.scannedCodes.toMutableList().apply { remove(barcode) }
            state.copy(scannedCodes = updatedCodes.toImmutableList())
        }
        updateCalculatedSum()
    }

    fun onStartEditingProduct(code: String, name: String) {
        _state.update { state ->
            state.copy(
                editingCode = code,
                editingName = name
            )
        }
    }

    fun onEditingNameChanged(name: String) {
        _state.update { state ->
            state.copy(editingName = name)
        }
    }

    fun onSaveEditedProduct() {
        _state.update { state ->
            val code = state.editingCode ?: return@update state
            val currentResult = state.scannedProducts[code]
            val updatedProducts = state.scannedProducts.toMutableMap().apply {
                put(code, ScanResult(
                    barcode = code,
                    productName = state.editingName,
                    category = currentResult?.category ?: "Others",
                    price = currentResult?.price,
                    currency = currentResult?.currency
                ))
            }
            state.copy(
                scannedProducts = updatedProducts.toImmutableMap(),
                editingCode = null,
                editingName = ""
            )
        }
        updateCalculatedSum()
    }

    fun onCancelEditingProduct() {
        _state.update { state ->
            state.copy(
                editingCode = null,
                editingName = ""
            )
        }
    }

    private fun updateCalculatedSum() {
        val currentState = _state.value
        val sum = currentState.scannedCodes.sumOf { code ->
            currentState.scannedProducts[code]?.price ?: 24.50
        }
        _state.update { state ->
            if (!state.isAmountEditedByUser) {
                val amountStr = if (sum > 0) String.format(java.util.Locale.US, "%.2f", sum) else ""
                state.copy(commonAmountString = amountStr)
            } else {
                state
            }
        }
    }

    fun saveBatchTransaction() {
        val currentState = _state.value
        if (currentState.scannedCodes.isEmpty()) return

        _state.update { state -> state.copy(isSaving = true) }

        viewModelScope.launch {
            val finalCodes = if (currentState.addBarcodeOnce) {
                currentState.scannedCodes.distinct()
            } else {
                currentState.scannedCodes.toList()
            }

            // Look up any missing barcodes from backend as a safety net
            val scannedProductsMutable = currentState.scannedProducts.toMutableMap()
            val missingBarcodes = finalCodes.filter { it !in scannedProductsMutable }
            if (missingBarcodes.isNotEmpty()) {
                try {
                    val results = scanRepository.lookupBatchBarcodes(missingBarcodes)
                    results.forEach {
                        if (it.barcode != null) {
                            scannedProductsMutable[it.barcode] = it
                        }
                    }
                } catch (e: Exception) {
                    // Safe fallback
                }
            }

            val savedSum = finalCodes.sumOf { scannedProductsMutable[it]?.price ?: 24.50 }
            val totalAmount = currentState.commonAmountString.toDoubleOrNull() ?: savedSum
            val transactionTitle = if (currentState.commonTitle.isNotBlank()) {
                currentState.commonTitle
            } else {
                val names = finalCodes.mapNotNull { code ->
                    val name = scannedProductsMutable[code]?.productName
                    if (isProductValid(name, code)) name else null
                }.distinct()
                if (names.isNotEmpty()) names.joinToString(", ") else "Batch Scanned Items (${finalCodes.size})"
            }

            val firstCategoryStr = finalCodes.mapNotNull { scannedProductsMutable[it]?.category }.firstOrNull()
            val category = if (firstCategoryStr != null) {
                TransactionCategory.fromString(firstCategoryStr)
            } else {
                TransactionCategory.GROCERIES
            }

            val barcodeDetails = finalCodes.joinToString("; ") { code ->
                val name = scannedProductsMutable[code]?.productName
                val displayName = if (isProductValid(name, code)) name!! else "Unknown Product"
                "$displayName ($code)"
            }
            val description = "Batch scanned barcodes: $barcodeDetails"

            val transactionTime = System.currentTimeMillis()
            val transaction = Transaction(
                id = UUID.randomUUID().toString(),
                title = transactionTitle.take(50),
                timestamp = transactionTime,
                amount = -totalAmount,
                icon = category.icon,
                iconBgColor = category.iconBgColor,
                type = TransactionType.EXPENSE,
                category = category,
                description = description,
                isSynced = false,
                formattedDate = com.hevincj.cashflow.utils.DateTimeUtils.formatTimestamp(transactionTime)
            )
            try {
                transactionRepository.insertTransaction(transaction)
                _state.update { state -> state.copy(saveSuccess = true, isSaving = false) }
            } catch (e: Exception) {
                _state.update { state -> state.copy(errorMessage = "Failed to save transaction: ${e.message}", isSaving = false) }
            }
        }
    }

    fun analyzeReceipt(bytes: ByteArray, onResult: (ReceiptScanResult?) -> Unit) {
        _state.update { state -> state.copy(isAnalyzing = true) }
        viewModelScope.launch {
            val result = analyzeReceiptUseCase(bytes)
            _state.update { state -> state.copy(isAnalyzing = false) }
            onResult(result)
        }
    }

    fun analyzeReceiptWithDetails(
        bytes: ByteArray,
        onOutcome: (com.hevincj.cashflow.domain.models.ReceiptAnalysisOutcome) -> Unit
    ) {
        _state.update { state -> state.copy(isAnalyzing = true) }
        viewModelScope.launch {
            val outcome = analyzeReceiptUseCase.analyze(bytes)
            _state.update { state -> state.copy(isAnalyzing = false) }
            onOutcome(outcome)
        }
    }

    fun lookupSingleBarcode(barcode: String, onResult: (ScanResult?) -> Unit) {
        viewModelScope.launch {
            val result = scanRepository.lookupBarcode(barcode)
            onResult(result)
        }
    }

    fun generateUpiQr(amount: Double?, note: String?, onResult: (UpiQrResult?) -> Unit) {
        viewModelScope.launch {
            val result = scanRepository.generateUpiQr(amount, note)
            onResult(result)
        }
    }

    fun sendUpiPayment(
        vpa: String,
        name: String,
        amount: Double,
        note: String?,
        rrn: String? = null,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val finalRrn = rrn ?: (System.currentTimeMillis().toString().takeLast(8) + (1000..9999).random().toString())
                val description = "Sent via UPI\nTo: $name ($vpa)\nRef: $finalRrn\nNote: ${note ?: "—"}"
                val transactionTime = System.currentTimeMillis()
                val transaction = Transaction(
                    id = java.util.UUID.randomUUID().toString(),
                    title = "UPI · $name",
                    timestamp = transactionTime,
                    amount = -amount,
                    icon = TransactionCategory.OTHERS.icon,
                    iconBgColor = TransactionCategory.OTHERS.iconBgColor,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.OTHERS,
                    description = description,
                    isSynced = false,
                    formattedDate = com.hevincj.cashflow.utils.DateTimeUtils.formatTimestamp(transactionTime)
                )
                transactionRepository.insertTransaction(transaction)
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }
}

