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

    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(themeManager.themeMode).thenReturn(MutableStateFlow(ThemeMode.SYSTEM))
        kotlinx.coroutines.runBlocking {
            whenever(userRepository.getUserProfile()).thenReturn(
                Result.success(UserProfile("leslie@gmail.com", "Leslie", "Alexander", "123456", null))
            )
        }
        viewModel = ProfileViewModel(authRepository, themeManager, transactionRepository, userRepository)
    }

    @Test
    fun testLogoutCallsRepositoryAndUpdatesState() = runTest {
        viewModel.logout()
        advanceUntilIdle()

        verify(authRepository).logout()
        assertTrue(viewModel.state.value.isLoggedOut)
    }
}
