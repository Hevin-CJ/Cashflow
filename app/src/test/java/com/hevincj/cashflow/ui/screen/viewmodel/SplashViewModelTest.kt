package com.hevincj.cashflow.ui.screen.viewmodel

import com.hevincj.cashflow.domain.repository.AuthRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class SplashViewModelTest {

    @Mock
    lateinit var authRepository: AuthRepository

    private lateinit var viewModel: SplashViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = SplashViewModel(authRepository)
    }

    @Test
    fun testIsUserLoggedInReturnsTrueWhenTokenExists() {
        whenever(authRepository.isLoggedIn()).thenReturn(true)
        assertTrue(viewModel.isUserLoggedIn())
    }

    @Test
    fun testIsUserLoggedInReturnsFalseWhenTokenIsNull() {
        whenever(authRepository.isLoggedIn()).thenReturn(false)
        assertFalse(viewModel.isUserLoggedIn())
    }
}
