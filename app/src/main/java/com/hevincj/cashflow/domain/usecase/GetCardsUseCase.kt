package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.models.CreditCard
import com.hevincj.cashflow.domain.repository.CreditCardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCardsUseCase @Inject constructor(
    private val repository: CreditCardRepository
) {
    operator fun invoke(): Flow<List<CreditCard>> = repository.getCards()
}
