package com.hevincj.cashflow.data.remote.api

import com.hevincj.cashflow.data.remote.models.BudgetDto
import com.hevincj.cashflow.data.remote.models.BudgetRequestDto
import retrofit2.Response
import retrofit2.http.*

interface BudgetApi {

    @GET("/api/budgets")
    suspend fun getBudgets(): Response<List<BudgetDto>>

    @POST("/api/budgets")
    suspend fun createBudget(@Body request: BudgetRequestDto): Response<BudgetDto>

    @PUT("/api/budgets/{id}")
    suspend fun updateBudget(
        @Path("id") id: String,
        @Body request: BudgetRequestDto
    ): Response<Unit>

    @DELETE("/api/budgets/{id}")
    suspend fun deleteBudget(@Path("id") id: String): Response<Unit>
}
