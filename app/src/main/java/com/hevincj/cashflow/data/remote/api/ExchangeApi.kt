package com.hevincj.cashflow.data.remote.api

import com.hevincj.cashflow.data.remote.models.ExchangeResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ExchangeApi {
    @GET("latest")
    suspend fun getLatestRates(
        @Query("from") from: String
    ): Response<ExchangeResponseDto>
}

