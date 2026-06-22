package com.hevincj.cashflow.data.remote.api

import com.hevincj.cashflow.data.remote.models.CreditCardDto
import com.hevincj.cashflow.data.remote.models.CreditCardRequestDto
import retrofit2.Response
import retrofit2.http.*

interface CardsApi {

    @GET("/api/cards")
    suspend fun getCards(): Response<List<CreditCardDto>>

    @POST("/api/cards")
    suspend fun createCard(@Body request: CreditCardRequestDto): Response<CreditCardDto>

    @DELETE("/api/cards/{id}")
    suspend fun deleteCard(@Path("id") id: String): Response<Unit>
}
