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
    fun testLoginDelegatesToRepositorySuccess() = runTest {
        whenever(repository.login("user", "pass")).thenReturn(Result.success(Unit))

        val result = loginUseCase("user", "pass")

        assertEquals(Result.success(Unit), result)
    }

    @Test
    fun testLoginDelegatesToRepositoryFailure() = runTest {
        val exception = Exception("Invalid credentials")
        whenever(repository.login("user", "pass")).thenReturn(Result.failure(exception))

        val result = loginUseCase("user", "pass")

        assertEquals(Result.failure<Unit>(exception), result)
    }
}
