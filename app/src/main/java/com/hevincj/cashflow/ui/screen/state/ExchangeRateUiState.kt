package com.hevincj.cashflow.ui.screen.state

data class ExchangeRateUiState(
    val isTopActive: Boolean = true,
    val topInputValue: String = "0",
    val bottomInputValue: String = "0",
    val currencyTop: String = "INR",
    val currencyBottom: String = "USD",
    val exchangeRates: Map<String, Double> = emptyMap(),
    val isLoading: Boolean = true,
    val lastUpdatedDate: String = "Loading rates..."
)
