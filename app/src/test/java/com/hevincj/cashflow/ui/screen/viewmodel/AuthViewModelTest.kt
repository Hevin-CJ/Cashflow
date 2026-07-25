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
        viewModel.onUsernameChange("testuser@example.com")
        viewModel.onPasswordChange("testpassword")
        viewModel.onFirstNameChange("John")
        viewModel.onLastNameChange("Doe")
        viewModel.onPhoneNumberChange("1234567890")
        viewModel.onOtpChange("123456")

        val state = viewModel.state.value
        assertEquals("testuser@example.com", state.username)
        assertEquals("testpassword", state.password)
        assertEquals("John", state.firstName)
        assertEquals("Doe", state.lastName)
        assertEquals("1234567890", state.phoneNumber)
        assertEquals("123456", state.otp)
    }

    @Test
    fun testInitiateLoginSuccessUpdatesState() = runTest {
        viewModel.onUsernameChange("testuser@example.com")
        viewModel.onPasswordChange("testpass")
        whenever(loginUseCase.initiate(any(), any())).thenReturn(Result.success(Unit))

        viewModel.initiateLogin()
        advanceUntilIdle()

        assertEquals(AuthState.OtpSentLogin, viewModel.state.value.authState)
    }

    @Test
    fun testVerifyLoginSuccessUpdatesState() = runTest {
        viewModel.onUsernameChange("testuser@example.com")
        viewModel.onOtpChange("123456")
        whenever(loginUseCase.verify(any(), any())).thenReturn(Result.success(Unit))

        viewModel.verifyLogin()
        advanceUntilIdle()

        assertEquals(AuthState.LoginSuccess, viewModel.state.value.authState)
    }

    @Test
    fun testInitiateRegisterSuccessUpdatesState() = runTest {
        viewModel.onUsernameChange("testuser@example.com")
        viewModel.onPasswordChange("testpass")
        viewModel.onFirstNameChange("John")
        viewModel.onLastNameChange("Doe")
        viewModel.onPhoneNumberChange("1234567890")
        whenever(registerUseCase.initiate(any(), any(), any(), any(), any())).thenReturn(Result.success(Unit))

        viewModel.initiateRegister()
        advanceUntilIdle()

        assertEquals(AuthState.OtpSentRegister, viewModel.state.value.authState)
    }

    @Test
    fun testVerifyRegisterSuccessUpdatesState() = runTest {
        viewModel.onUsernameChange("testuser@example.com")
        viewModel.onOtpChange("123456")
        whenever(registerUseCase.verify(any(), any())).thenReturn(Result.success(Unit))

        viewModel.verifyRegister()
        advanceUntilIdle()

        assertEquals(AuthState.RegisterSuccess, viewModel.state.value.authState)
    }

    @Test
    fun testResetStateResetsToIdle() {
        viewModel.resetState()
        assertEquals(AuthState.Idle, viewModel.state.value.authState)
    }
}
