package com.hevincj.cashflow.ui.screen.state

data class AuthUiState(
    val username: String = "",
    val password: String = "",
    val authState: AuthState = AuthState.Idle
)
