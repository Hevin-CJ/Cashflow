package com.hevincj.cashflow.ui.screen.state

import androidx.compose.runtime.Immutable
import com.hevincj.cashflow.domain.models.CreditCard
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class CardsUiState(
    val cards: ImmutableList<CreditCard> = persistentListOf(),
    val isLoading: Boolean = false,
    val error: String? = null
)
