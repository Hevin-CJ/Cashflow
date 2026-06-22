package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.models.CreditCard
import com.hevincj.cashflow.domain.repository.CreditCardRepository
import javax.inject.Inject

class AddCardUseCase @Inject constructor(
    private val repository: CreditCardRepository
) {
    suspend operator fun invoke(card: CreditCard) {
        repository.addCard(card)
    }
}
