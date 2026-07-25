package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.repository.ExchangeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExchangeRatesUseCase @Inject constructor(
    private val repository: ExchangeRepository
) {
    operator fun invoke(base: String): Flow<Map<String, Double>> {
        return repository.observeRates(base)
    }
}
