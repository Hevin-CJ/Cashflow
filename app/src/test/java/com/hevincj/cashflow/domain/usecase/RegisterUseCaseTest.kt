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
class RegisterUseCaseTest {

    @Mock
    lateinit var repository: AuthRepository

    private lateinit var registerUseCase: RegisterUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        registerUseCase = RegisterUseCase(repository)
    }

    @Test
    fun testRegisterDelegatesToRepositorySuccess() = runTest {
        whenever(repository.register("user", "pass")).thenReturn(Result.success(Unit))

        val result = registerUseCase("user", "pass")

        assertEquals(Result.success(Unit), result)
    }

    @Test
    fun testRegisterDelegatesToRepositoryFailure() = runTest {
        val exception = Exception("Username already exists")
        whenever(repository.register("user", "pass")).thenReturn(Result.failure(exception))

        val result = registerUseCase("user", "pass")

        assertEquals(Result.failure<Unit>(exception), result)
    }
}
