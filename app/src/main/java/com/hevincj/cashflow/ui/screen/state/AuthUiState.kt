package com.hevincj.cashflow.ui.screen.state

import androidx.compose.runtime.Immutable

@Immutable
data class AuthUiState(
    val username: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val otp: String = "",
    val authState: AuthState = AuthState.Idle
)
