package com.hevincj.cashflow.ui.screen.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.usecase.AddTransactionUseCase
import com.hevincj.cashflow.ui.screen.state.AddTransactionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.lifecycle.SavedStateHandle
import com.hevincj.cashflow.domain.usecase.GetTransactionByIdUseCase
import com.hevincj.cashflow.domain.models.RecurringExpense
import com.hevincj.cashflow.domain.models.RecurringFrequency
import com.hevincj.cashflow.domain.repository.RecurringExpenseRepository
import com.hevincj.cashflow.domain.usecase.AddRecurringExpenseUseCase
import com.hevincj.cashflow.domain.usecase.DeleteRecurringExpenseUseCase
import com.hevincj.cashflow.domain.usecase.GetTransactionsUseCase
import com.hevincj.cashflow.domain.usecase.UpdateTransactionUseCase
import kotlinx.coroutines.flow.first

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val addRecurringExpenseUseCase: AddRecurringExpenseUseCase,
    private val deleteRecurringExpenseUseCase: DeleteRecurringExpenseUseCase,
    private val recurringExpenseRepository: RecurringExpenseRepository,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getTransactionByIdUseCase: GetTransactionByIdUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
 
    private val _state = MutableStateFlow(AddTransactionUiState())
    val state: StateFlow<AddTransactionUiState> = _state.asStateFlow()
 
    private var scannedBarcode: String? = null
 
    init {
        val txId: String? = savedStateHandle["transactionId"]
        if (txId != null) {
            _state.value = AddTransactionUiState(
                isEditMode = true,
                isLoading = true,
                transactionId = txId
            )
            loadTransactionForEdit(txId)
        } else {
            val prefillTitle: String? = savedStateHandle["title"]
            val prefillAmount: String? = savedStateHandle["amount"]
            val prefillCategory: String? = savedStateHandle["category"]
            val prefillDescription: String? = savedStateHandle["description"]
            val prefillBarcode: String? = savedStateHandle["barcode"]
 
            scannedBarcode = prefillBarcode
 
            val sanitizedAmount = prefillAmount?.let { amt ->
                var dotFound = false
                val cleaned = amt.filter { char ->
                    if (char == '.') {
                        if (!dotFound) {
                            dotFound = true
                            true
                        } else {
                            false
                        }
                    } else char.isDigit()
                }
                val dotIndex = cleaned.indexOf('.')
                val limited = if (dotIndex != -1) {
                    val before = cleaned.substring(0, dotIndex)
                    val after = cleaned.substring(dotIndex + 1)
                    val afterLimited = if (after.length > 2) after.substring(0, 2) else after
                    "$before.$afterLimited"
                } else {
                    cleaned
                }
                val doubleVal = limited.toDoubleOrNull()
                if (doubleVal != null && doubleVal > 999999.0) {
                    "999999.00"
                } else {
                    limited
                }
            } ?: ""
 
            if (prefillTitle != null || prefillAmount != null || prefillCategory != null || prefillDescription != null || prefillBarcode != null) {
                val parsedCategory = prefillCategory?.let {
                    TransactionCategory.fromString(it)
                } ?: TransactionCategory.FOOD
 
                val combinedDescription = when {
                    prefillTitle != null && !prefillDescription.isNullOrBlank() -> "$prefillTitle - $prefillDescription"
                    prefillTitle != null -> prefillTitle
                    !prefillDescription.isNullOrBlank() -> prefillDescription
                    else -> ""
                }
 
                _state.value = AddTransactionUiState(
                    title = prefillTitle ?: "",
                    amount = sanitizedAmount,
                    category = parsedCategory,
                    description = combinedDescription,
                    type = TransactionType.EXPENSE
                )
            }
        }
    }
 
    private fun loadTransactionForEdit(txId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val transaction = getTransactionByIdUseCase(txId)
                if (transaction != null) {
                    val loadedAmount = kotlin.math.abs(transaction.amount)
                    val cappedAmount = if (loadedAmount > 999999.0) 999999.0 else loadedAmount
                    val formattedAmount = String.format(java.util.Locale.US, "%.2f", cappedAmount)
                        .replace(Regex("\\.00$"), "")
                        .replace(Regex("(\\.[1-9])0$"), "$1")

                    var isRecurring = false
                    var frequency = RecurringFrequency.MONTHLY
                    try {
                        val activeRecurring = recurringExpenseRepository.getActiveRecurringExpenses()
                        val matching = if (!transaction.recurringExpenseId.isNullOrBlank()) {
                            activeRecurring.firstOrNull {
                                it.id == transaction.recurringExpenseId ||
                                it.localId.toString() == transaction.recurringExpenseId ||
                                (it.serverId != null && it.serverId == transaction.recurringExpenseId)
                            }
                        } else {
                            // Fallback heuristic for transactions created before relation ID linking
                            activeRecurring.firstOrNull { sub ->
                                sub.transaction.title.equals(transaction.title, ignoreCase = true) &&
                                sub.transaction.category == transaction.category
                            }
                        }
                        if (matching != null) {
                            isRecurring = true
                            frequency = matching.frequency
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    _state.value = _state.value.copy(
                        title = transaction.title,
                        amount = formattedAmount,
                        type = transaction.type,
                        category = transaction.category,
                        description = transaction.description ?: "",
                        isRecurring = isRecurring,
                        recurringFrequency = frequency,
                        isEditMode = true,
                        transactionId = txId,
                        isLoading = false
                    )
                } else {
                    _state.value = _state.value.copy(
                        errorMessage = "Transaction not found",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    errorMessage = e.localizedMessage ?: "Failed to load transaction",
                    isLoading = false
                )
            }
        }
    }
 
    fun onAmountChange(value: String) {
        if (value.isEmpty()) {
            _state.value = _state.value.copy(amount = value, errorMessage = null)
        } else if (value.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            val doubleVal = value.toDoubleOrNull()
            if (doubleVal == null || doubleVal <= 999999.0) {
                _state.value = _state.value.copy(amount = value, errorMessage = null)
            }
        }
    }
 
    fun onTypeChange(value: TransactionType) {
        val currentCategory = _state.value.category
        val updatedCategory = if (currentCategory.supportedTypes.contains(value)) {
            currentCategory
        } else {
            TransactionCategory.values().first { it.supportedTypes.contains(value) }
        }
        _state.value = _state.value.copy(type = value, category = updatedCategory)
    }
 
    fun onCategoryChange(value: TransactionCategory) {
        _state.value = _state.value.copy(category = value)
    }
 
    fun onDescriptionChange(value: String) {
        _state.value = _state.value.copy(description = value)
    }

    fun onRecurringChange(isRecurring: Boolean) {
        _state.value = _state.value.copy(isRecurring = isRecurring)
    }

    fun onRecurringFrequencyChange(frequency: RecurringFrequency) {
        _state.value = _state.value.copy(recurringFrequency = frequency)
    }

    fun saveTransaction() {
        val currentState = _state.value
        val amt = currentState.amount.toDoubleOrNull()
        if (amt == null || amt <= 0) {
            _state.value = _state.value.copy(errorMessage = "Please enter a valid amount")
            return
        }
        if (amt > 999999.0) {
            _state.value = _state.value.copy(errorMessage = "Amount cannot exceed $999,999.00")
            return
        }
 
        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
 
        viewModelScope.launch {
            try {
                val icon = currentState.category.icon
                val iconBgColor = currentState.category.iconBgColor
 
                val finalAmount = if (currentState.type == TransactionType.EXPENSE) -amt else amt
 
                val finalDescription = currentState.description
                val finalTitle = if (currentState.title.isNotBlank()) currentState.title else (if (currentState.description.isNotBlank()) currentState.description else currentState.category.displayName)
 
                if (currentState.isEditMode && currentState.transactionId != null) {
                    val originalTx = getTransactionByIdUseCase(currentState.transactionId)
                    val originalTimestamp = originalTx?.timestamp ?: System.currentTimeMillis()

                    // Look for existing recurring blueprint if linked
                    val activeSubs = recurringExpenseRepository.getActiveRecurringExpenses()
                    val existingSub = if (!originalTx?.recurringExpenseId.isNullOrBlank()) {
                        activeSubs.firstOrNull {
                            it.id == originalTx?.recurringExpenseId ||
                            it.localId.toString() == originalTx?.recurringExpenseId ||
                            (it.serverId != null && it.serverId == originalTx?.recurringExpenseId)
                        }
                    } else null

                    var targetRecurringId = originalTx?.recurringExpenseId

                    if (currentState.isRecurring) {
                        if (existingSub != null) {
                            val updatedSub = existingSub.copy(
                                frequency = currentState.recurringFrequency,
                                transaction = Transaction(
                                    id = existingSub.transaction.id,
                                    title = finalTitle,
                                    timestamp = originalTimestamp,
                                    amount = finalAmount,
                                    icon = icon,
                                    iconBgColor = iconBgColor,
                                    type = currentState.type,
                                    category = currentState.category,
                                    description = finalDescription,
                                    isSynced = false
                                )
                            )
                            recurringExpenseRepository.updateRecurringExpense(updatedSub)
                        } else {
                            val nextDue = when (currentState.recurringFrequency) {
                                RecurringFrequency.DAILY -> System.currentTimeMillis() + 86400000L
                                RecurringFrequency.WEEKLY -> System.currentTimeMillis() + 7 * 86400000L
                                RecurringFrequency.MONTHLY -> System.currentTimeMillis() + 30 * 86400000L
                                RecurringFrequency.YEARLY -> System.currentTimeMillis() + 365 * 86400000L
                            }
                            val newSubscription = RecurringExpense(
                                id = "",
                                localId = 0,
                                frequency = currentState.recurringFrequency,
                                startDate = originalTimestamp,
                                lastProcessedDate = originalTimestamp,
                                nextDueDate = nextDue,
                                isSynced = false,
                                transaction = Transaction(
                                    id = "",
                                    title = finalTitle,
                                    timestamp = originalTimestamp,
                                    amount = finalAmount,
                                    icon = icon,
                                    iconBgColor = iconBgColor,
                                    type = currentState.type,
                                    category = currentState.category,
                                    description = finalDescription,
                                    isSynced = false
                                )
                            )
                            addRecurringExpenseUseCase(newSubscription)
                        }
                    } else {
                        if (existingSub != null) {
                            deleteRecurringExpenseUseCase(existingSub)
                            targetRecurringId = null
                        }
                    }

                    val updatedTransaction = Transaction(
                        id = currentState.transactionId,
                        title = finalTitle,
                        timestamp = originalTimestamp,
                        amount = finalAmount,
                        icon = icon,
                        iconBgColor = iconBgColor,
                        type = currentState.type,
                        category = currentState.category,
                        description = finalDescription,
                        isSynced = false,
                        barcode = scannedBarcode,
                        productName = if (scannedBarcode != null) (if (currentState.title.isNotBlank()) currentState.title.trim() else null) else null,
                        formattedDate = com.hevincj.cashflow.utils.DateTimeUtils.formatTimestamp(originalTimestamp),
                        lastModifiedLocal = System.currentTimeMillis(),
                        recurringExpenseId = targetRecurringId
                    )
                    updateTransactionUseCase(updatedTransaction)
                } else {
                    val currentTimestamp = System.currentTimeMillis()
                    var assignedRecurringId: String? = null

                    if (currentState.isRecurring) {
                        val nextDue = when (currentState.recurringFrequency) {
                            RecurringFrequency.DAILY -> currentTimestamp + 86400000L
                            RecurringFrequency.WEEKLY -> currentTimestamp + 7 * 86400000L
                            RecurringFrequency.MONTHLY -> currentTimestamp + 30 * 86400000L
                            RecurringFrequency.YEARLY -> currentTimestamp + 365 * 86400000L
                        }
                        val tempTx = Transaction(
                            id = "",
                            title = finalTitle,
                            timestamp = currentTimestamp,
                            amount = finalAmount,
                            icon = icon,
                            iconBgColor = iconBgColor,
                            type = currentState.type,
                            category = currentState.category,
                            description = finalDescription,
                            isSynced = false
                        )
                        val newSubscription = RecurringExpense(
                            id = "",
                            localId = 0,
                            frequency = currentState.recurringFrequency,
                            startDate = currentTimestamp,
                            lastProcessedDate = currentTimestamp,
                            nextDueDate = nextDue,
                            isSynced = false,
                            transaction = tempTx
                        )
                        val localId = addRecurringExpenseUseCase(newSubscription)
                        if (localId > 0) {
                            assignedRecurringId = localId.toString()
                        }
                    }

                    val newTransaction = Transaction(
                        id = "0", // Autogenerated by Room
                        title = finalTitle,
                        timestamp = currentTimestamp,
                        amount = finalAmount,
                        icon = icon,
                        iconBgColor = iconBgColor,
                        type = currentState.type,
                        category = currentState.category,
                        description = finalDescription,
                        isSynced = false,
                        barcode = scannedBarcode,
                        productName = if (scannedBarcode != null) (if (currentState.title.isNotBlank()) currentState.title.trim() else null) else null,
                        formattedDate = com.hevincj.cashflow.utils.DateTimeUtils.formatTimestamp(currentTimestamp),
                        lastModifiedLocal = System.currentTimeMillis(),
                        recurringExpenseId = assignedRecurringId
                    )
                    addTransactionUseCase(newTransaction)
                }

                _state.value = _state.value.copy(isSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(errorMessage = e.localizedMessage ?: "Failed to save transaction")
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }
}
