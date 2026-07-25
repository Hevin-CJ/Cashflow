package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.repository.AuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LoginUseCaseTest {

    @Mock
    lateinit var repository: AuthRepository

    private lateinit var loginUseCase: LoginUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        loginUseCase = LoginUseCase(repository)
    }

    @Test
    fun testLoginInitiateDelegatesToRepositorySuccess() = runTest {
        whenever(repository.initiateLogin("user", "pass")).thenReturn(Result.success(Unit))

        val result = loginUseCase.initiate("user", "pass")

        assertEquals(Result.success(Unit), result)
    }

    @Test
    fun testLoginInitiateDelegatesToRepositoryFailure() = runTest {
        val exception = Exception("Invalid credentials")
        whenever(repository.initiateLogin("user", "pass")).thenReturn(Result.failure(exception))

        val result = loginUseCase.initiate("user", "pass")

        assertEquals(Result.failure<Unit>(exception), result)
    }

    @Test
    fun testLoginVerifyDelegatesToRepositorySuccess() = runTest {
        whenever(repository.verifyLogin("user", "123456")).thenReturn(Result.success(Unit))

        val result = loginUseCase.verify("user", "123456")

        assertEquals(Result.success(Unit), result)
    }

    @Test
    fun testLoginVerifyDelegatesToRepositoryFailure() = runTest {
        val exception = Exception("Invalid OTP")
        whenever(repository.verifyLogin("user", "123456")).thenReturn(Result.failure(exception))

        val result = loginUseCase.verify("user", "123456")

        assertEquals(Result.failure<Unit>(exception), result)
    }
}
