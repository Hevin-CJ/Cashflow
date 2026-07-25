package com.hevincj.cashflow.data.remote.api

import com.hevincj.cashflow.data.remote.models.LoginRequestDto
import com.hevincj.cashflow.data.remote.models.LoginResponseDto
import com.hevincj.cashflow.data.remote.models.RegisterRequestDto
import com.hevincj.cashflow.data.remote.models.RegisterResponseDto
import com.hevincj.cashflow.data.remote.models.VerifyOtpRequestDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("/api/users/register/initiate")
    suspend fun registerInitiate(@Body request: RegisterRequestDto): Response<ResponseBody>

    @POST("/api/users/register/verify")
    suspend fun registerVerify(@Body request: VerifyOtpRequestDto): Response<LoginResponseDto>

    @POST("/api/users/login/initiate")
    suspend fun loginInitiate(@Body request: LoginRequestDto): Response<ResponseBody>

    @POST("/api/users/login/verify")
    suspend fun loginVerify(@Body request: VerifyOtpRequestDto): Response<LoginResponseDto>
}