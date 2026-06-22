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
    fun testLoginSuccessSavesTokenAndReturnsSuccess() = runTest {
        val loginResponseDto = LoginResponseDto("1", "user", "my_jwt_token")
        val successResponse = Response.success(loginResponseDto)
        whenever(api.login(any())).thenReturn(successResponse)

        val result = repository.login("user", "pass")

        assertTrue(result.isSuccess)
        verify(tokenManager).saveToken("my_jwt_token")
    }

    @Test
    fun testLoginSuccessWithNoTokenReturnsFailure() = runTest {
        val loginResponseDto = LoginResponseDto("1", "user", "")
        // Wait, what if token field is empty, or let's mock response body containing null token if possible?
        // In our DTO, token is non-nullable String. But body itself can be null.
        val successResponse = Response.success<LoginResponseDto>(null)
        whenever(api.login(any())).thenReturn(successResponse)

        val result = repository.login("user", "pass")

        assertTrue(result.isFailure)
        assertEquals("No token in response", result.exceptionOrNull()?.message)
    }

    @Test
    fun testLoginFailureReturnsError() = runTest {
        val errorBody = "Invalid Credentials".toResponseBody("application/json".toMediaTypeOrNull())
        val errorResponse = Response.error<LoginResponseDto>(400, errorBody)
        whenever(api.login(any())).thenReturn(errorResponse)

        val result = repository.login("user", "pass")

        assertTrue(result.isFailure)
        assertEquals("Invalid Credentials", result.exceptionOrNull()?.message)
    }

    @Test
    fun testLoginExceptionReturnsFailure() = runTest {
        val exception = RuntimeException("Network Error")
        whenever(api.login(any())).thenThrow(exception)

        val result = repository.login("user", "pass")

        assertTrue(result.isFailure)
        assertEquals("Network Error", result.exceptionOrNull()?.message)
    }

    @Test
    fun testRegisterSuccessReturnsSuccess() = runTest {
        val registerResponseDto = RegisterResponseDto("1", "user")
        val successResponse = Response.success(registerResponseDto)
        whenever(api.register(any())).thenReturn(successResponse)

        val result = repository.register("user", "pass")

        assertTrue(result.isSuccess)
    }

    @Test
    fun testRegisterFailureReturnsError() = runTest {
        val errorBody = "Username exists".toResponseBody("application/json".toMediaTypeOrNull())
        val errorResponse = Response.error<RegisterResponseDto>(409, errorBody)
        whenever(api.register(any())).thenReturn(errorResponse)

        val result = repository.register("user", "pass")

        assertTrue(result.isFailure)
        assertEquals("Username exists", result.exceptionOrNull()?.message)
    }

    @Test
    fun testLogoutClearsTokenAndDatabase() = runTest {
        repository.logout()

        verify(tokenManager).clearToken()
        verify(database).clearAllTables()
    }
}
