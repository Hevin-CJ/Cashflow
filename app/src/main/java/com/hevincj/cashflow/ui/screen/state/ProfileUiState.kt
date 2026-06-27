package com.hevincj.cashflow.ui.screen.state

import androidx.compose.runtime.Immutable
import com.hevincj.cashflow.data.local.ThemeMode

@Immutable
data class ProfileUiState(
    val isLoggedOut: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
