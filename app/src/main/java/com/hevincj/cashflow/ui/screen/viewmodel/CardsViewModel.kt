package com.hevincj.cashflow.ui.screen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hevincj.cashflow.domain.models.CreditCard
import com.hevincj.cashflow.domain.repository.CreditCardRepository
import com.hevincj.cashflow.domain.usecase.GetCardsUseCase
import com.hevincj.cashflow.domain.usecase.AddCardUseCase
import com.hevincj.cashflow.ui.screen.state.CardsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList

@HiltViewModel
class CardsViewModel @Inject constructor(
    private val getCardsUseCase: GetCardsUseCase,
    private val addCardUseCase: AddCardUseCase,
    private val repository: CreditCardRepository,
    private val networkMonitor: com.hevincj.cashflow.utils.NetworkMonitor
) : ViewModel() {

    private val _state = MutableStateFlow(CardsUiState())
    val state: StateFlow<CardsUiState> = _state.asStateFlow()

    init {
        loadCards()
        observeNetworkChanges()
    }

    private fun observeNetworkChanges() {
        viewModelScope.launch {
            var isFirstEmission = true
            networkMonitor.isConnected
                .distinctUntilChanged()
                .collect { isConnected ->
                    if (isConnected) {
                        if (isFirstEmission) {
                            // Skip the initial startup emission; loadCards() already handles
                            // the first load. Only re-sync when connectivity is restored
                            // after a prior offline period.
                            isFirstEmission = false
                            return@collect
                        }
                        val currentError = _state.value.error
                        if (currentError == "No internet connection") {
                            _state.value = _state.value.copy(error = null)
                            syncCards()
                        }
                    } else {
                        isFirstEmission = false
                        _state.value = _state.value.copy(
                            error = "No internet connection",
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun loadCards() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            getCardsUseCase().collect { cards ->
                _state.value = _state.value.copy(
                    cards = cards.toImmutableList(),
                    isLoading = false
                )
            }
        }
    }

    fun addCard(cardHolder: String, cardNumber: String, balance: Double, gradientColors: List<Long>) {
        viewModelScope.launch {
            val card = CreditCard(
                id = "",
                balance = balance,
                cardNumber = cardNumber,
                cardHolder = cardHolder,
                gradientColors = gradientColors.toImmutableList()
            )
            addCardUseCase(card)
        }
    }

    fun syncCards() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val syncError = repository.syncCards()
            _state.value = _state.value.copy(error = syncError, isLoading = false)
        }
    }
}
