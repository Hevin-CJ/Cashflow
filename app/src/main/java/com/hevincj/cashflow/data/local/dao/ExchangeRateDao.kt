package com.hevincj.cashflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hevincj.cashflow.data.local.entity.ExchangeRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rates WHERE baseCurrency = :base")
    suspend fun getRatesForBase(base: String): List<ExchangeRateEntity>

    @Query("SELECT * FROM exchange_rates WHERE baseCurrency = :base")
    fun observeRatesForBase(base: String): Flow<List<ExchangeRateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<ExchangeRateEntity>)
}
