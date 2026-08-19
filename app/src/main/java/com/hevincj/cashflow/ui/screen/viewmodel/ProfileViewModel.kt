package com.hevincj.cashflow.ui.screen.viewmodel

import androidx.lifecycle.ViewModel
import com.hevincj.cashflow.domain.repository.AuthRepository
import com.hevincj.cashflow.data.local.ThemeManager
import com.hevincj.cashflow.data.local.ThemeMode
import com.hevincj.cashflow.domain.repository.TransactionRepository
import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.ui.screen.state.ProfileUiState
import com.hevincj.cashflow.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.hevincj.cashflow.domain.repository.UpdateRepository
import com.hevincj.cashflow.utils.ApkDownloader
import com.hevincj.cashflow.utils.DownloadStatus
import com.hevincj.cashflow.BuildConfig

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val themeManager: ThemeManager,
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository,
    private val updateRepository: UpdateRepository,
    private val apkDownloader: ApkDownloader
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun checkForUpdates(isManualCheck: Boolean = true) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isCheckingUpdate = true, updateMessage = null)
            val currentVersion = BuildConfig.VERSION_NAME
            updateRepository.checkForUpdate(currentVersion)
                .onSuccess { updateInfo ->
                    val isAvailable = updateInfo.isUpdateAvailable
                    _state.value = _state.value.copy(
                        isCheckingUpdate = false,
                        hasUpdateAvailable = isAvailable,
                        latestAvailableVersion = if (isAvailable) updateInfo.latestVersion else null,
                        availableUpdateInfo = if (isAvailable) updateInfo else null,
                        updateInfo = if (isAvailable && isManualCheck) updateInfo else _state.value.updateInfo,
                        updateMessage = if (!isAvailable && isManualCheck) {
                            "You are on the latest version (v$currentVersion)"
                        } else null
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isCheckingUpdate = false,
                        updateMessage = if (isManualCheck) "Failed to check for updates: ${error.localizedMessage ?: "Network error"}" else null
                    )
                }
        }
    }

    fun openUpdateDialog() {
        val available = _state.value.availableUpdateInfo
        if (available != null) {
            _state.value = _state.value.copy(updateInfo = available)
        } else {
            checkForUpdates(isManualCheck = true)
        }
    }

    fun startDownload(updateInfo: com.hevincj.cashflow.domain.models.AppUpdateInfo) {
        viewModelScope.launch {
            apkDownloader.downloadUpdate(updateInfo).collect { status ->
                _state.value = _state.value.copy(downloadStatus = status)
            }
        }
    }

    fun startDownload(downloadUrl: String, versionName: String) {
        val currentInfo = _state.value.updateInfo ?: com.hevincj.cashflow.domain.models.AppUpdateInfo(
            isUpdateAvailable = true,
            latestVersion = versionName,
            currentVersion = "",
            releaseTitle = "",
            releaseNotes = "",
            downloadUrl = downloadUrl,
            apkSize = 0L
        )
        startDownload(currentInfo)
    }

    fun dismissUpdateDialog() {
        _state.value = _state.value.copy(
            updateInfo = null,
            downloadStatus = DownloadStatus.Idle
        )
    }

    fun clearUpdateMessage() {
        _state.value = _state.value.copy(updateMessage = null)
    }

    init {
        viewModelScope.launch {
            themeManager.themeMode.collect { mode ->
                _state.value = _state.value.copy(themeMode = mode)
            }
        }
        viewModelScope.launch {
            userRepository.getUserProfileFlow().collect { profile ->
                if (profile != null) {
                    _state.value = _state.value.copy(
                        username = profile.username,
                        firstName = profile.firstName ?: "",
                        lastName = profile.lastName ?: "",
                        phoneNumber = profile.phoneNumber ?: "",
                        profileImage = profile.profileImage?.takeIf { it.isNotEmpty() }
                    )
                }
            }
        }
        fetchUserProfile()
        checkForUpdates(isManualCheck = false)
    }

    fun fetchUserProfile() {
        viewModelScope.launch {
            if (_state.value.username.isEmpty()) {
                _state.value = _state.value.copy(isLoading = true, error = null)
            }
            userRepository.getUserProfile()
                .onSuccess { profile ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        username = profile.username,
                        firstName = profile.firstName ?: "",
                        lastName = profile.lastName ?: "",
                        phoneNumber = profile.phoneNumber ?: "",
                        profileImage = profile.profileImage?.takeIf { it.isNotEmpty() }
                    )
                }
                .onFailure { exception ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = if (_state.value.username.isEmpty()) (exception.message ?: "Failed to fetch user profile") else null
                    )
                }
        }
    }

    fun updateProfile(firstName: String, lastName: String, phoneNumber: String, profileImage: String?) {
        val cleanProfileImage = profileImage?.takeIf { it.isNotEmpty() }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, isUpdateSuccess = false)
            userRepository.updateProfile(firstName, lastName, phoneNumber, cleanProfileImage)
                .onSuccess { profile ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isUpdateSuccess = true,
                        firstName = profile.firstName ?: "",
                        lastName = profile.lastName ?: "",
                        phoneNumber = profile.phoneNumber ?: "",
                        profileImage = profile.profileImage?.takeIf { it.isNotEmpty() }
                    )
                }
                .onFailure { exception ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to update profile"
                    )
                }
        }
    }

    fun clearUpdateSuccess() {
        _state.value = _state.value.copy(isUpdateSuccess = false)
    }

    fun setThemeMode(mode: ThemeMode) {
        themeManager.setThemeMode(mode)
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _state.value = _state.value.copy(isLoggedOut = true)
        }
    }

    suspend fun getTransactionsForRange(range: ExportDateRange): List<Transaction> {
        val allTransactions = transactionRepository.getAllTransactionsList()
        if (range == ExportDateRange.ALL) return allTransactions

        val zoneId = java.time.ZoneId.systemDefault()
        val now = java.time.LocalDate.now(zoneId)
        val currentMonth = java.time.YearMonth.from(now)
        val currentYear = java.time.Year.from(now)

        val (startMs, endMs) = when (range) {
            ExportDateRange.CURRENT_MONTH -> {
                val start = currentMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val end = currentMonth.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                start to end
            }
            ExportDateRange.PREVIOUS_MONTH -> {
                val prevMonth = currentMonth.minusMonths(1)
                val start = prevMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val end = currentMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                start to end
            }
            ExportDateRange.CURRENT_YEAR -> {
                val start = currentYear.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val end = currentYear.plusYears(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                start to end
            }
            ExportDateRange.PREVIOUS_YEAR -> {
                val prevYear = currentYear.minusYears(1)
                val start = prevYear.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val end = currentYear.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                start to end
            }
            ExportDateRange.ALL -> 0L to 0L
        }

        return allTransactions.filter { it.timestamp in startMs until endMs }
    }
}

enum class ExportDateRange {
    CURRENT_MONTH,
    PREVIOUS_MONTH,
    CURRENT_YEAR,
    PREVIOUS_YEAR,
    ALL
}
