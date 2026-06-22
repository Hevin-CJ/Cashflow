package com.hevincj.cashflow.ui.screen.state

import com.hevincj.cashflow.domain.models.CreditCard

data class CardsUiState(
    val cards: List<CreditCard> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
