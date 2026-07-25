package com.hevincj.cashflow.ui.screen.state

import androidx.compose.runtime.Immutable
import com.hevincj.cashflow.data.local.ThemeMode

@Immutable
data class ProfileUiState(
    val isLoggedOut: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val profileImage: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isUpdateSuccess: Boolean = false
)
