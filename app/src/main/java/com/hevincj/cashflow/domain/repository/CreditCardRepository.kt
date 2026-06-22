package com.hevincj.cashflow.domain.repository

import com.hevincj.cashflow.domain.models.CreditCard
import kotlinx.coroutines.flow.Flow

interface CreditCardRepository {
    fun getCards(): Flow<List<CreditCard>>
    suspend fun addCard(card: CreditCard)
    suspend fun syncCards(): String?
}
