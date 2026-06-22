package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.models.CreditCard
import com.hevincj.cashflow.domain.repository.CreditCardRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetCardsUseCaseTest {

    @Mock
    lateinit var repository: CreditCardRepository

    private lateinit var getCardsUseCase: GetCardsUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        getCardsUseCase = GetCardsUseCase(repository)
    }

    @Test
    fun testGetCardsDelegatesToRepository() = runTest {
        val sampleCards = listOf(
            CreditCard(
                id = "1",
                balance = 500.0,
                cardNumber = "1234 5678 9012 3456",
                cardHolder = "John Doe",
                gradientColors = listOf(0xFF111111L, 0xFF222222L)
            )
        )

        whenever(repository.getCards()).thenReturn(flowOf(sampleCards))

        val result = getCardsUseCase().first()

        assertEquals(sampleCards, result)
    }
}
