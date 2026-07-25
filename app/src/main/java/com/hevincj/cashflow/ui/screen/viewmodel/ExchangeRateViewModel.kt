package com.hevincj.cashflow.ui.screen.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hevincj.cashflow.domain.usecase.GetExchangeRatesUseCase
import com.hevincj.cashflow.domain.usecase.RefreshExchangeRatesUseCase
import com.hevincj.cashflow.ui.screen.state.ExchangeRateUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

import com.hevincj.cashflow.utils.NetworkMonitor
import kotlinx.coroutines.flow.distinctUntilChanged

@HiltViewModel
class ExchangeRateViewModel @Inject constructor(
    private val getExchangeRatesUseCase: GetExchangeRatesUseCase,
    private val refreshExchangeRatesUseCase: RefreshExchangeRatesUseCase,
    private val networkMonitor: NetworkMonitor,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ExchangeRateUiState())
    val state: StateFlow<ExchangeRateUiState> = _state.asStateFlow()

    private var ratesCollectionJob: Job? = null
    private var isNetworkConnected = false

    init {
        observeNetworkChanges()
    }

    private fun observeNetworkChanges() {
        viewModelScope.launch {
            networkMonitor.isConnected
                .distinctUntilChanged()
                .collect { isConnected ->
                    isNetworkConnected = isConnected
                    if (isConnected) {
                        fetchRates()
                    } else {
                        val baseCurrency = _state.value.currencyTop
                        // Start database observation immediately even if offline (offline-first baseline)
                        observeRates(baseCurrency)
                        
                        val sharedPrefs = context.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)
                        val cachedTime = sharedPrefs.getString("last_updated_$baseCurrency", null)
                        
                        _state.value = _state.value.copy(
                            isLoading = false,
                            lastUpdatedDate = cachedTime ?: "Offline (using cached rates)"
                        )
                    }
                }
        }
    }

    fun fetchRates() {
        viewModelScope.launch {
            val baseCurrency = _state.value.currencyTop
            
            // Start observing rates from local Room database immediately (Offline-First)
            observeRates(baseCurrency)
            
            val sharedPrefs = context.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)
            val cachedTime = sharedPrefs.getString("last_updated_$baseCurrency", null)
            
            if (!isNetworkConnected) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    lastUpdatedDate = cachedTime ?: "Offline (using cached rates)"
                )
                return@launch
            }
            
            _state.value = _state.value.copy(isLoading = true)
            
            // Trigger remote API query to refresh rates cache
            val result = refreshExchangeRatesUseCase(baseCurrency)
            
            if (result.isSuccess) {
                val timeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy hh:mm:ss a")
                val formattedTime = LocalDateTime.now().format(timeFormatter)
                val statusText = "Last updated: $formattedTime"
                
                sharedPrefs.edit().putString("last_updated_$baseCurrency", statusText).apply()
                
                _state.value = _state.value.copy(
                    lastUpdatedDate = statusText,
                    isLoading = false
                )
            } else {
                _state.value = _state.value.copy(
                    lastUpdatedDate = cachedTime ?: "Offline (using cached rates)",
                    isLoading = false
                )
            }
        }
    }

    private fun observeRates(baseCurrency: String) {
        ratesCollectionJob?.cancel()
        ratesCollectionJob = viewModelScope.launch {
            getExchangeRatesUseCase(baseCurrency).collect { rates ->
                _state.value = _state.value.copy(
                    exchangeRates = rates
                )
            }
        }
    }

    fun setTopActive(active: Boolean) {
        val currentState = _state.value
        if (currentState.isTopActive == active) return

        val rates = currentState.exchangeRates
        val currencyTop = currentState.currencyTop
        val currencyBottom = currentState.currencyBottom
        val currentRate = if (currencyTop == currencyBottom) {
            1.0
        } else {
            rates[currencyBottom] ?: getFallbackRate(currencyTop, currencyBottom)
        }

        if (active) {
            val parsed = currentState.bottomInputValue.toDoubleOrNull() ?: 0.0
            val converted = if (currentRate != 0.0) parsed / currentRate else 0.0
            val formatted = formatConvertedInput(converted)
            _state.value = currentState.copy(
                isTopActive = true,
                topInputValue = formatted
            )
        } else {
            val parsed = currentState.topInputValue.toDoubleOrNull() ?: 0.0
            val converted = parsed * currentRate
            val formatted = formatConvertedInput(converted)
            _state.value = currentState.copy(
                isTopActive = false,
                bottomInputValue = formatted
            )
        }
    }

    private fun formatConvertedInput(value: Double): String {
        if (value == 0.0) return "0"
        val raw = String.format(Locale.US, "%.4f", value)
        var cleaned = raw
        if (cleaned.contains(".")) {
            cleaned = cleaned.trimEnd('0').trimEnd('.')
        }
        return if (cleaned.isEmpty()) "0" else cleaned
    }

    private fun getFallbackRate(from: String, to: String): Double {
        if (from == to) return 1.0
        val ratesFromInr = mapOf(
            "INR" to 1.0,
            "USD" to 0.012,
            "EUR" to 0.011,
            "GBP" to 0.0094,
            "JPY" to 1.86,
            "AUD" to 0.018,
            "CAD" to 0.016
        )
        val fromRate = ratesFromInr[from] ?: 1.0
        val toRate = ratesFromInr[to] ?: 1.0
        return toRate / fromRate
    }

    fun updateTopInput(value: String) {
        _state.value = _state.value.copy(topInputValue = value)
    }

    fun updateBottomInput(value: String) {
        _state.value = _state.value.copy(bottomInputValue = value)
    }

    fun setCurrencyTop(code: String) {
        _state.value = _state.value.copy(
            currencyTop = code,
            topInputValue = "0",
            bottomInputValue = "0"
        )
        fetchRates()
    }

    fun setCurrencyBottom(code: String) {
        _state.value = _state.value.copy(
            currencyBottom = code,
            topInputValue = "0",
            bottomInputValue = "0"
        )
    }

    override fun onCleared() {
        super.onCleared()
        ratesCollectionJob?.cancel()
    }
}
