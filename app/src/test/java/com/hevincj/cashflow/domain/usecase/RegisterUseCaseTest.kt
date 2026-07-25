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
    fun testRegisterInitiateDelegatesToRepositorySuccess() = runTest {
        whenever(repository.initiateRegister("user", "pass", "John", "Doe", "1234567890")).thenReturn(Result.success(Unit))

        val result = registerUseCase.initiate("user", "pass", "John", "Doe", "1234567890")

        assertEquals(Result.success(Unit), result)
    }

    @Test
    fun testRegisterInitiateDelegatesToRepositoryFailure() = runTest {
        val exception = Exception("Username already exists")
        whenever(repository.initiateRegister("user", "pass", "John", "Doe", "1234567890")).thenReturn(Result.failure(exception))

        val result = registerUseCase.initiate("user", "pass", "John", "Doe", "1234567890")

        assertEquals(Result.failure<Unit>(exception), result)
    }

    @Test
    fun testRegisterVerifyDelegatesToRepositorySuccess() = runTest {
        whenever(repository.verifyRegister("user", "123456")).thenReturn(Result.success(Unit))

        val result = registerUseCase.verify("user", "123456")

        assertEquals(Result.success(Unit), result)
    }

    @Test
    fun testRegisterVerifyDelegatesToRepositoryFailure() = runTest {
        val exception = Exception("Invalid OTP")
        whenever(repository.verifyRegister("user", "123456")).thenReturn(Result.failure(exception))

        val result = registerUseCase.verify("user", "123456")

        assertEquals(Result.failure<Unit>(exception), result)
    }
}
