package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.repository.ExchangeRepository
import javax.inject.Inject

class RefreshExchangeRatesUseCase @Inject constructor(
    private val repository: ExchangeRepository
) {
    suspend operator fun invoke(base: String): Result<Unit> {
        return repository.refreshRates(base)
    }
}
