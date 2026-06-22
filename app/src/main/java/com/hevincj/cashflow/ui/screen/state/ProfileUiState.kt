package com.hevincj.cashflow.ui.screen.state

import com.hevincj.cashflow.data.local.ThemeMode

data class ProfileUiState(
    val isLoggedOut: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
