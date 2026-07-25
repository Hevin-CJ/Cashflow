package com.hevincj.cashflow.data.remote.api

import com.hevincj.cashflow.data.remote.models.UpdateProfileRequestDto
import com.hevincj.cashflow.data.remote.models.UserProfileResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface UserApi {

    @GET("/api/users/profile")
    suspend fun getUserProfile(): Response<UserProfileResponseDto>

    @PUT("/api/users/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequestDto): Response<UserProfileResponseDto>
}
