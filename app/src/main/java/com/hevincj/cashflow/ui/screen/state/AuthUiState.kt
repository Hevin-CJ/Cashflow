package com.hevincj.cashflow.ui.screen.state

import androidx.compose.runtime.Immutable

@Immutable
data class AuthUiState(
    val username: String = "",
    val password: String = "",
    val authState: AuthState = AuthState.Idle
)
