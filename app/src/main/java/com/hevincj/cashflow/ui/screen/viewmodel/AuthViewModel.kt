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

    fun login() {
        val currentState = _state.value
        if (currentState.username.isBlank() || currentState.password.isBlank()) {
            _state.value = _state.value.copy(authState = AuthState.Error("Username and password cannot be empty"))
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(authState = AuthState.Loading)
            val result = loginUseCase(currentState.username, currentState.password)
            if (result.isSuccess) {
                _state.value = _state.value.copy(authState = AuthState.LoginSuccess)
            } else {
                _state.value = _state.value.copy(authState = AuthState.Error(result.exceptionOrNull()?.message ?: "Login failed"))
            }
        }
    }

    fun register() {
        val currentState = _state.value
        if (currentState.username.isBlank() || currentState.password.isBlank()) {
            _state.value = _state.value.copy(authState = AuthState.Error("Username and password cannot be empty"))
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(authState = AuthState.Loading)
            val result = registerUseCase(currentState.username, currentState.password)
            if (result.isSuccess) {
                _state.value = _state.value.copy(authState = AuthState.RegisterSuccess)
            } else {
                _state.value = _state.value.copy(authState = AuthState.Error(result.exceptionOrNull()?.message ?: "Registration failed"))
            }
        }
    }

    fun resetState() {
        _state.value = _state.value.copy(authState = AuthState.Idle)
    }
}
