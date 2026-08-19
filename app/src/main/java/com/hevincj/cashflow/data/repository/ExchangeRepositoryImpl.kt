package com.hevincj.cashflow.data.repository

import com.hevincj.cashflow.data.local.dao.ExchangeRateDao
import com.hevincj.cashflow.data.local.entity.ExchangeRateEntity
import com.hevincj.cashflow.data.remote.api.ExchangeApi
import com.hevincj.cashflow.domain.repository.ExchangeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExchangeRepositoryImpl @Inject constructor(
    private val api: ExchangeApi,
    private val dao: ExchangeRateDao
) : ExchangeRepository {

    override fun observeRates(base: String): Flow<Map<String, Double>> {
        return dao.observeRatesForBase(base).map { entities ->
            entities.associate { it.targetCurrency to it.rate }
        }
    }

    override suspend fun refreshRates(base: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.getLatestRates(base)
            if (response.isSuccessful) {
                val body = response.body() ?: throw Exception("Empty response body")
                
                // Persist rates in local database cache
                val entities = body.rates.map { (target, rate) ->
                    ExchangeRateEntity(
                        baseCurrency = base,
                        targetCurrency = target,
                        rate = rate
                    )
                }
                dao.insertRates(entities)
                
                Result.success(Unit)
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                com.hevincj.cashflow.utils.CrashLogger.w("ExchangeRepository", "Refresh rates failed for base $base: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("ExchangeRepository", "Exception refreshing rates for base $base: ${e.message}", e)
            Result.failure(e)
        }
    }
}
