package com.hevincj.cashflow.ui.screen.viewmodel

import androidx.lifecycle.ViewModel
import com.hevincj.cashflow.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    fun isUserLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }
}
