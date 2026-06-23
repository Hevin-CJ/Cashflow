package com.hevincj.cashflow.data.remote.api

import com.hevincj.cashflow.data.remote.models.RecurringExpenseDto
import com.hevincj.cashflow.data.remote.models.RecurringExpenseRequestDto
import retrofit2.Response
import retrofit2.http.*

interface RecurringExpenseApi {

    @GET("/api/recurring")
    suspend fun getRecurringExpenses(): Response<List<RecurringExpenseDto>>

    @POST("/api/recurring")
    suspend fun createRecurringExpense(@Body request: RecurringExpenseRequestDto): Response<RecurringExpenseDto>

    @PUT("/api/recurring/{id}")
    suspend fun updateRecurringExpense(
        @Path("id") id: String,
        @Body request: RecurringExpenseRequestDto
    ): Response<Unit>

    @DELETE("/api/recurring/{id}")
    suspend fun deleteRecurringExpense(@Path("id") id: String): Response<Unit>
}
