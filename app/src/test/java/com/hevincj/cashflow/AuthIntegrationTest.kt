package com.hevincj.cashflow

import com.hevincj.cashflow.data.remote.api.AuthApi
import com.hevincj.cashflow.data.remote.models.LoginRequestDto
import com.hevincj.cashflow.data.remote.models.RegisterRequestDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthIntegrationTest {

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://127.0.0.1:8080/") // Ktor backend running locally
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val authApi = retrofit.create(AuthApi::class.java)

    @Test
    @org.junit.Ignore("Requires local Ktor backend running on 127.0.0.1:8080")
    fun testRegisterAndLoginIntegration() = runBlocking {
        val uniqueUsername = "user_${System.currentTimeMillis()}"
        val password = "securepassword123"

        // 1. Test Registration
        val registerRequest = RegisterRequestDto(uniqueUsername, password)
        val registerResponse = authApi.registerInitiate(registerRequest)

        assertTrue("Register response should be successful", registerResponse.isSuccessful)

        // 2. Test Login
        val loginRequest = LoginRequestDto(uniqueUsername, password)
        val loginResponse = authApi.loginInitiate(loginRequest)

        assertTrue("Login response should be successful", loginResponse.isSuccessful)
    }
}
