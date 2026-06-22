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
        val registerResponse = authApi.register(registerRequest)

        assertTrue("Register response should be successful", registerResponse.isSuccessful)
        val registerBody = registerResponse.body()
        assertNotNull("Register body should not be null", registerBody)
        assertEquals("Registered username should match", uniqueUsername, registerBody?.username)
        assertNotNull("Registered user should have a valid database ID", registerBody?.id)

        println("Successfully registered user in local MongoDB. ID: ${registerBody?.id}")

        // 2. Test Login
        val loginRequest = LoginRequestDto(uniqueUsername, password)
        val loginResponse = authApi.login(loginRequest)

        assertTrue("Login response should be successful", loginResponse.isSuccessful)
        val loginBody = loginResponse.body()
        assertNotNull("Login body should not be null", loginBody)
        assertEquals("Logged in username should match", uniqueUsername, loginBody?.username)
        assertNotNull("Logged in user should have a valid database ID", loginBody?.id)
        assertNotNull("Login response should contain a signed JWT token", loginBody?.token)
        assertTrue("JWT token should not be empty", loginBody?.token?.isNotEmpty() == true)

        println("Successfully authenticated user against Ktor backend. Generated JWT: ${loginBody?.token}")
    }
}
