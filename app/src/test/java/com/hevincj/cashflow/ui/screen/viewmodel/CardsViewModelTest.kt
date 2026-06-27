package com.hevincj.cashflow.ui.screen.viewmodel

import com.hevincj.cashflow.MainDispatcherRule
import com.hevincj.cashflow.domain.models.CreditCard
import com.hevincj.cashflow.domain.repository.CreditCardRepository
import com.hevincj.cashflow.domain.usecase.GetCardsUseCase
import com.hevincj.cashflow.domain.usecase.AddCardUseCase
import com.hevincj.cashflow.utils.NetworkMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalCoroutinesApi::class)
class CardsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    lateinit var getCardsUseCase: GetCardsUseCase

    @Mock
    lateinit var addCardUseCase: AddCardUseCase

    @Mock
    lateinit var repository: CreditCardRepository

    @Mock
    lateinit var networkMonitor: NetworkMonitor

    private lateinit var viewModel: CardsViewModel

    private val sampleCards = listOf(
        CreditCard(
            id = "1",
            balance = 1000.00,
            cardNumber = "1111 2222 3333 4444",
            cardHolder = "John Doe",
            gradientColors = persistentListOf(0xFF67E2AEL, 0xFFE8679AL)
        )
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(networkMonitor.isConnected).thenReturn(flowOf(true))
    }

    @Test
    fun testInitializationLoadsCardsAndDoesNotPopulateIfNotEmpty() = runTest {
        whenever(getCardsUseCase.invoke()).thenReturn(flowOf(sampleCards))
        
        viewModel = CardsViewModel(getCardsUseCase, addCardUseCase, repository, networkMonitor)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(sampleCards, state.cards)
        verify(repository, never()).addCard(any())
    }
}
