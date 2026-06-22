package com.hevincj.cashflow.data.remote.api

import com.hevincj.cashflow.data.remote.models.LoginRequestDto
import com.hevincj.cashflow.data.remote.models.LoginResponseDto
import com.hevincj.cashflow.data.remote.models.RegisterRequestDto
import com.hevincj.cashflow.data.remote.models.RegisterResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("/api/users/login")
    suspend fun login(@Body request: LoginRequestDto): Response<LoginResponseDto>

    @POST("/api/users/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<RegisterResponseDto>
}