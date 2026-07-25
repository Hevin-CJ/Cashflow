package com.hevincj.cashflow.ui.screen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hevincj.cashflow.domain.usecase.LoginUseCase
import com.hevincj.cashflow.domain.usecase.RegisterUseCase
import com.hevincj.cashflow.ui.screen.state.AuthState
import com.hevincj.cashflow.ui.screen.state.AuthUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
private fun String.isValidEmail(): Boolean = this.matches(emailRegex)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onUsernameChange(value: String) {
        _state.value = _state.value.copy(username = value)
    }

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value)
    }

    fun onFirstNameChange(value: String) {
        _state.value = _state.value.copy(firstName = value)
    }

    fun onLastNameChange(value: String) {
        _state.value = _state.value.copy(lastName = value)
    }

    fun onPhoneNumberChange(value: String) {
        _state.value = _state.value.copy(phoneNumber = value)
    }

    fun onOtpChange(value: String) {
        _state.value = _state.value.copy(otp = value)
    }

    fun initiateLogin() {
        val currentState = _state.value
        if (currentState.username.isBlank() || currentState.password.isBlank()) {
            _state.value = _state.value.copy(authState = AuthState.Error("Username and password cannot be empty"))
            return
        }
        if (!currentState.username.isValidEmail()) {
            _state.value = _state.value.copy(authState = AuthState.Error("Please enter a valid email address"))
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(authState = AuthState.Loading)
            val result = loginUseCase.initiate(currentState.username, currentState.password)
            if (result.isSuccess) {
                _state.value = _state.value.copy(authState = AuthState.OtpSentLogin)
            } else {
                _state.value = _state.value.copy(authState = AuthState.Error(result.exceptionOrNull()?.message ?: "Login failed"))
            }
        }
    }

    fun verifyLogin() {
        val currentState = _state.value
        if (currentState.otp.isBlank()) {
            _state.value = _state.value.copy(authState = AuthState.Error("OTP code cannot be empty"))
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(authState = AuthState.Loading)
            val result = loginUseCase.verify(currentState.username, currentState.otp)
            if (result.isSuccess) {
                _state.value = _state.value.copy(authState = AuthState.LoginSuccess)
            } else {
                _state.value = _state.value.copy(authState = AuthState.Error(result.exceptionOrNull()?.message ?: "Invalid OTP code"))
            }
        }
    }

    fun initiateRegister() {
        val currentState = _state.value
        if (currentState.username.isBlank() || currentState.password.isBlank()) {
            _state.value = _state.value.copy(authState = AuthState.Error("Username and password cannot be empty"))
            return
        }
        if (!currentState.username.isValidEmail()) {
            _state.value = _state.value.copy(authState = AuthState.Error("Please enter a valid email address"))
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(authState = AuthState.Loading)
            val result = registerUseCase.initiate(
                currentState.username, 
                currentState.password,
                currentState.firstName.takeIf { it.isNotBlank() },
                currentState.lastName.takeIf { it.isNotBlank() },
                currentState.phoneNumber.takeIf { it.isNotBlank() }
            )
            if (result.isSuccess) {
                _state.value = _state.value.copy(authState = AuthState.OtpSentRegister)
            } else {
                _state.value = _state.value.copy(authState = AuthState.Error(result.exceptionOrNull()?.message ?: "Registration failed"))
            }
        }
    }

    fun verifyRegister() {
        val currentState = _state.value
        if (currentState.otp.isBlank()) {
            _state.value = _state.value.copy(authState = AuthState.Error("OTP code cannot be empty"))
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(authState = AuthState.Loading)
            val result = registerUseCase.verify(currentState.username, currentState.otp)
            if (result.isSuccess) {
                _state.value = _state.value.copy(authState = AuthState.RegisterSuccess)
            } else {
                _state.value = _state.value.copy(authState = AuthState.Error(result.exceptionOrNull()?.message ?: "Invalid OTP code"))
            }
        }
    }

    fun resetState() {
        _state.value = _state.value.copy(authState = AuthState.Idle)
    }
}
