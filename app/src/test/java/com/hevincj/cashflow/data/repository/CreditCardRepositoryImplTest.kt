package com.hevincj.cashflow.data.repository

import com.hevincj.cashflow.data.local.dao.CreditCardDao
import com.hevincj.cashflow.data.local.entity.CreditCardEntity
import com.hevincj.cashflow.data.remote.api.CardsApi
import com.hevincj.cashflow.domain.models.CreditCard
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CreditCardRepositoryImplTest {

    @Mock
    lateinit var dao: CreditCardDao

    @Mock
    lateinit var api: CardsApi

    private lateinit var repository: CreditCardRepositoryImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        repository = CreditCardRepositoryImpl(dao, api)
    }

    @Test
    fun testGetCardsMapsEntitiesToDomain() = runTest {
        val entities = listOf(
            CreditCardEntity(
                id = 1,
                balance = 1000.0,
                cardNumber = "1111 2222 3333 4444",
                cardHolder = "John Doe",
                gradientColorsHex = "ff67e2ae,ffe8679a"
            )
        )

        whenever(dao.getCards()).thenReturn(flowOf(entities))

        val cards = repository.getCards().first()

        assertEquals(1, cards.size)
        val card = cards[0]
        assertEquals("1", card.id)
        assertEquals(1000.0, card.balance, 0.01)
        assertEquals("1111 2222 3333 4444", card.cardNumber)
        assertEquals("John Doe", card.cardHolder)
        assertEquals(2, card.gradientColors.size)
        assertEquals(0xff67e2aeL, card.gradientColors[0])
        assertEquals(0xffe8679aL, card.gradientColors[1])
    }

    @Test
    fun testAddCardMapsDomainToEntityAndInserts() = runTest {
        whenever(dao.getCards()).thenReturn(flowOf(emptyList()))

        val domainCard = CreditCard(
            id = "2",
            balance = 500.0,
            cardNumber = "5555 6666 7777 8888",
            cardHolder = "Jane Doe",
            gradientColors = listOf(0xff123456L, 0xff789abcL)
        )

        repository.addCard(domainCard)

        val captor = argumentCaptor<CreditCardEntity>()
        verify(dao).insertCard(captor.capture())

        val capturedEntity = captor.firstValue
        assertEquals(2, capturedEntity.id)
        assertEquals(500.0, capturedEntity.balance, 0.01)
        assertEquals("5555 6666 7777 8888", capturedEntity.cardNumber)
        assertEquals("Jane Doe", capturedEntity.cardHolder)
        assertEquals("ff123456,ff789abc", capturedEntity.gradientColorsHex)
    }
}
