package com.hevincj.cashflow.data.remote.api

import com.hevincj.cashflow.data.remote.models.TransactionDto
import com.hevincj.cashflow.data.remote.models.TransactionRequestDto
import retrofit2.Response
import retrofit2.http.*

interface TransactionApi {

    @GET("/api/transactions")
    suspend fun getTransactions(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<List<TransactionDto>>

    @POST("/api/transactions")
    suspend fun createTransaction(@Body request: TransactionRequestDto): Response<TransactionDto>

    @PUT("/api/transactions/{id}")
    suspend fun updateTransaction(
        @Path("id") id: String,
        @Body request: TransactionRequestDto
    ): Response<Unit>

    @DELETE("/api/transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: String): Response<Unit>
}