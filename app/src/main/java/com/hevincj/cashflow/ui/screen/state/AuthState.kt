package com.hevincj.cashflow.ui.screen.state

import androidx.compose.runtime.Immutable

@Immutable
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object LoginSuccess : AuthState()
    object RegisterSuccess : AuthState()
    object OtpSentLogin : AuthState()
    object OtpSentRegister : AuthState()
    data class Error(val message: String) : AuthState()
}
