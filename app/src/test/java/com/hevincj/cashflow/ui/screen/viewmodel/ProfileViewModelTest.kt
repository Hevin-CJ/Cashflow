package com.hevincj.cashflow.ui.screen.viewmodel

import com.hevincj.cashflow.MainDispatcherRule
import com.hevincj.cashflow.domain.repository.AuthRepository
import com.hevincj.cashflow.data.local.ThemeManager
import com.hevincj.cashflow.data.local.ThemeMode
import com.hevincj.cashflow.domain.repository.TransactionRepository
import com.hevincj.cashflow.domain.repository.UserRepository
import com.hevincj.cashflow.domain.models.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    lateinit var authRepository: AuthRepository

    @Mock
    lateinit var themeManager: ThemeManager

    @Mock
    lateinit var transactionRepository: TransactionRepository

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var updateRepository: com.hevincj.cashflow.domain.repository.UpdateRepository

    @Mock
    lateinit var apkDownloader: com.hevincj.cashflow.utils.ApkDownloader

    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(themeManager.themeMode).thenReturn(MutableStateFlow(ThemeMode.SYSTEM))
        whenever(userRepository.getUserProfileFlow()).thenReturn(
            kotlinx.coroutines.flow.flowOf(UserProfile("leslie@gmail.com", "Leslie", "Alexander", "123456", null))
        )
        kotlinx.coroutines.runBlocking {
            whenever(userRepository.getUserProfile()).thenReturn(
                Result.success(UserProfile("leslie@gmail.com", "Leslie", "Alexander", "123456", null))
            )
            whenever(updateRepository.checkForUpdate(org.mockito.kotlin.any())).thenReturn(
                Result.success(
                    com.hevincj.cashflow.domain.models.AppUpdateInfo(
                        isUpdateAvailable = false,
                        latestVersion = "1.0.0",
                        currentVersion = "1.0.0",
                        releaseTitle = "1.0.0",
                        releaseNotes = "",
                        downloadUrl = "",
                        apkSize = 0L
                    )
                )
            )
        }
        viewModel = ProfileViewModel(
            authRepository,
            themeManager,
            transactionRepository,
            userRepository,
            updateRepository,
            apkDownloader
        )
    }

    @Test
    fun testLogoutCallsRepositoryAndUpdatesState() = runTest {
        viewModel.logout()
        advanceUntilIdle()

        verify(authRepository).logout()
        assertTrue(viewModel.state.value.isLoggedOut)
    }

    @Test
    fun testUserProfileFlowPopulatesStateImmediately() = runTest {
        advanceUntilIdle()
        val state = viewModel.state.value
        org.junit.Assert.assertEquals("leslie@gmail.com", state.username)
        org.junit.Assert.assertEquals("Leslie", state.firstName)
        org.junit.Assert.assertEquals("Alexander", state.lastName)
        org.junit.Assert.assertEquals("123456", state.phoneNumber)
    }

    @Test
    fun testUpdateAvailablePopulatesState() = runTest {
        val updateInfo = com.hevincj.cashflow.domain.models.AppUpdateInfo(
            isUpdateAvailable = true,
            latestVersion = "1.0.9",
            currentVersion = "1.0.8",
            releaseTitle = "CashFlow v1.0.9",
            releaseNotes = "• New features",
            downloadUrl = "https://example.com/apk",
            apkSize = 1000L
        )
        whenever(updateRepository.checkForUpdate(org.mockito.kotlin.any())).thenReturn(
            Result.success(updateInfo)
        )

        viewModel.checkForUpdates(isManualCheck = false)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.hasUpdateAvailable)
        org.junit.Assert.assertEquals("1.0.9", state.latestAvailableVersion)
        org.junit.Assert.assertNotNull(state.availableUpdateInfo)
    }
}
