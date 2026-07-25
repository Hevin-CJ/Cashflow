package com.hevincj.cashflow.data.repository

import com.hevincj.cashflow.data.local.CashFlowDatabase
import com.hevincj.cashflow.data.local.TokenManager
import com.hevincj.cashflow.data.remote.api.AuthApi
import com.hevincj.cashflow.data.remote.models.LoginRequestDto
import com.hevincj.cashflow.data.remote.models.LoginResponseDto
import com.hevincj.cashflow.data.remote.models.RegisterRequestDto
import com.hevincj.cashflow.data.remote.models.RegisterResponseDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    @Mock
    lateinit var api: AuthApi

    @Mock
    lateinit var tokenManager: TokenManager

    @Mock
    lateinit var database: CashFlowDatabase

    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        repository = AuthRepositoryImpl(api, tokenManager, database)
    }

    @Test
    fun testInitiateLoginSuccess() = runTest {
        val successResponseBody = "".toResponseBody("text/plain".toMediaTypeOrNull())
        val successResponse = Response.success(successResponseBody)
        whenever(api.loginInitiate(any())).thenReturn(successResponse)

        val result = repository.initiateLogin("user", "pass")

        assertTrue(result.isSuccess)
    }

    @Test
    fun testInitiateLoginFailure() = runTest {
        val errorBody = "Invalid Credentials".toResponseBody("application/json".toMediaTypeOrNull())
        val errorResponse = Response.error<okhttp3.ResponseBody>(400, errorBody)
        whenever(api.loginInitiate(any())).thenReturn(errorResponse)

        val result = repository.initiateLogin("user", "pass")

        assertTrue(result.isFailure)
        assertEquals("Invalid Credentials", result.exceptionOrNull()?.message)
    }

    @Test
    fun testVerifyLoginSuccess() = runTest {
        val loginResponseDto = LoginResponseDto("1", "user", "my_jwt_token")
        val successResponse = Response.success(loginResponseDto)
        whenever(api.loginVerify(any())).thenReturn(successResponse)

        val result = repository.verifyLogin("user", "123456")

        assertTrue(result.isSuccess)
        verify(tokenManager).saveToken("my_jwt_token")
    }

    @Test
    fun testVerifyLoginNoToken() = runTest {
        val successResponse = Response.success<LoginResponseDto>(null)
        whenever(api.loginVerify(any())).thenReturn(successResponse)

        val result = repository.verifyLogin("user", "123456")

        assertTrue(result.isFailure)
        assertEquals("No token in response", result.exceptionOrNull()?.message)
    }

    @Test
    fun testInitiateRegisterSuccess() = runTest {
        val successResponseBody = "".toResponseBody("text/plain".toMediaTypeOrNull())
        val successResponse = Response.success(successResponseBody)
        whenever(api.registerInitiate(any())).thenReturn(successResponse)

        val result = repository.initiateRegister("user", "pass", "John", "Doe", "1234567890")

        assertTrue(result.isSuccess)
    }

    @Test
    fun testVerifyRegisterSuccess() = runTest {
        val loginResponseDto = LoginResponseDto("1", "user", "my_jwt_token")
        val successResponse = Response.success(loginResponseDto)
        whenever(api.registerVerify(any())).thenReturn(successResponse)

        val result = repository.verifyRegister("user", "123456")

        assertTrue(result.isSuccess)
        verify(tokenManager).saveToken("my_jwt_token")
    }

    @Test
    fun testLogoutClearsTokenAndDatabase() = runTest {
        repository.logout()

        verify(tokenManager).clearToken()
        verify(database).clearAllTables()
    }
}
