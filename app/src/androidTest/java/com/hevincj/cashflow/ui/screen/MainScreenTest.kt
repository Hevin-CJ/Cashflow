package com.hevincj.cashflow.ui.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavController
import com.hevincj.cashflow.domain.models.TransactionStats
import com.hevincj.cashflow.ui.screen.state.CardsUiState
import com.hevincj.cashflow.ui.screen.state.HomeUiState
import com.hevincj.cashflow.ui.screen.state.ProfileUiState
import com.hevincj.cashflow.ui.screen.state.StatsUiState
import com.hevincj.cashflow.ui.screen.viewmodel.CardsViewModel
import com.hevincj.cashflow.ui.screen.viewmodel.HomeViewModel
import com.hevincj.cashflow.ui.screen.viewmodel.ProfileViewModel
import com.hevincj.cashflow.ui.screen.viewmodel.StatsViewModel
import com.hevincj.cashflow.ui.screen.viewmodel.ScanViewModel
import com.hevincj.cashflow.domain.repository.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.YearMonth

class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val rootNavController = mock<NavController>()

    private val homeViewModel = mock<HomeViewModel>()
    private val statsViewModel = mock<StatsViewModel>()
    private val cardsViewModel = mock<CardsViewModel>()
    private val profileViewModel = mock<ProfileViewModel>()
    private val scanRepository = mock<ScanRepository>()
    private val scanViewModel = mock<ScanViewModel>()

    @org.junit.Before
    fun setUp() {
        whenever(scanViewModel.scanRepository).thenReturn(scanRepository)
    }

    // States
    private val homeStateFlow = MutableStateFlow(HomeUiState(isLoading = false))
    private val statsStateFlow = MutableStateFlow(StatsUiState(stats = TransactionStats(0.0, 0.0, emptyList(), emptyList(), emptyList()), isLoading = false))
    private val cardsStateFlow = MutableStateFlow(CardsUiState(cards = emptyList(), isLoading = false))
    private val profileStateFlow = MutableStateFlow(ProfileUiState(isLoggedOut = false))


    @Test
    fun testMainScreenBottomBarNavigationFlow() {
        // Setup states
        whenever(homeViewModel.state).thenReturn(homeStateFlow)
        whenever(statsViewModel.state).thenReturn(statsStateFlow)
        whenever(cardsViewModel.state).thenReturn(cardsStateFlow)
        whenever(profileViewModel.state).thenReturn(profileStateFlow)

        composeTestRule.setContent {
            MainScreen(
                rootNavController = rootNavController,
                homeViewModel = homeViewModel,
                statsViewModel = statsViewModel,
                cardsViewModel = cardsViewModel,
                profileViewModel = profileViewModel,
                scanViewModel = scanViewModel
            )
        }

        // Verify initial screen is HomeScreen (check dashboard element)
        composeTestRule.onNodeWithText("Transactions").assertIsDisplayed()

        // Navigate to Stats
        composeTestRule.onNodeWithContentDescription("Stats").performClick()
        composeTestRule.onNodeWithText("Statistics").assertIsDisplayed()

        // Navigate to Wallet
        composeTestRule.onNodeWithContentDescription("Wallet").performClick()
        composeTestRule.onNodeWithText("My Card").assertIsDisplayed()

        // Navigate to Profile
        composeTestRule.onNodeWithContentDescription("Profile").performClick()
        composeTestRule.onNodeWithText("Logout").assertIsDisplayed()
    }
}
