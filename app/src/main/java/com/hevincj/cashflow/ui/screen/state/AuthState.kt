package com.hevincj.cashflow.ui.screen.state

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object LoginSuccess : AuthState()
    object RegisterSuccess : AuthState()
    data class Error(val message: String) : AuthState()
}
