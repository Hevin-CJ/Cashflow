package com.hevincj.cashflow.ui.screen.viewmodel

import com.hevincj.cashflow.MainDispatcherRule
import com.hevincj.cashflow.domain.usecase.LoginUseCase
import com.hevincj.cashflow.domain.usecase.RegisterUseCase
import com.hevincj.cashflow.ui.screen.state.AuthState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    lateinit var loginUseCase: LoginUseCase

    @Mock
    lateinit var registerUseCase: RegisterUseCase

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = AuthViewModel(loginUseCase, registerUseCase)
    }

    @Test
    fun testUsernameAndPasswordChangeUpdatesState() {
        viewModel.onUsernameChange("testuser")
        viewModel.onPasswordChange("testpassword")

        val state = viewModel.state.value
        assertEquals("testuser", state.username)
        assertEquals("testpassword", state.password)
    }

    @Test
    fun testLoginSuccessUpdatesState() = runTest {
        viewModel.onUsernameChange("testuser")
        viewModel.onPasswordChange("testpass")
        whenever(loginUseCase.invoke(any(), any())).thenReturn(Result.success(Unit))

        viewModel.login()
        advanceUntilIdle()

        assertEquals(AuthState.LoginSuccess, viewModel.state.value.authState)
    }

    @Test
    fun testLoginFailureUpdatesErrorState() = runTest {
        viewModel.onUsernameChange("testuser")
        viewModel.onPasswordChange("testpass")
        whenever(loginUseCase.invoke(any(), any())).thenReturn(Result.failure(Exception("Invalid credentials")))

        viewModel.login()
        advanceUntilIdle()

        val authState = viewModel.state.value.authState
        assertTrue(authState is AuthState.Error)
        assertEquals("Invalid credentials", (authState as AuthState.Error).message)
    }

    @Test
    fun testRegisterSuccessUpdatesState() = runTest {
        viewModel.onUsernameChange("testuser")
        viewModel.onPasswordChange("testpass")
        whenever(registerUseCase.invoke(any(), any())).thenReturn(Result.success(Unit))

        viewModel.register()
        advanceUntilIdle()

        assertEquals(AuthState.RegisterSuccess, viewModel.state.value.authState)
    }

    @Test
    fun testRegisterFailureUpdatesErrorState() = runTest {
        viewModel.onUsernameChange("testuser")
        viewModel.onPasswordChange("testpass")
        whenever(registerUseCase.invoke(any(), any())).thenReturn(Result.failure(Exception("Registration failed")))

        viewModel.register()
        advanceUntilIdle()

        val authState = viewModel.state.value.authState
        assertTrue(authState is AuthState.Error)
        assertEquals("Registration failed", (authState as AuthState.Error).message)
    }

    @Test
    fun testResetStateResetsToIdle() {
        viewModel.resetState()
        assertEquals(AuthState.Idle, viewModel.state.value.authState)
    }
}
