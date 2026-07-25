package com.hevincj.cashflow.domain.repository

import kotlinx.coroutines.flow.Flow

interface ExchangeRepository {
    fun observeRates(base: String): Flow<Map<String, Double>>
    suspend fun refreshRates(base: String): Result<Unit>
}
