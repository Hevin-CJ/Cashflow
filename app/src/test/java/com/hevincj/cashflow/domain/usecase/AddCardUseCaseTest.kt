package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.models.CreditCard
import com.hevincj.cashflow.domain.repository.CreditCardRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalCoroutinesApi::class)
class AddCardUseCaseTest {

    @Mock
    lateinit var repository: CreditCardRepository

    private lateinit var addCardUseCase: AddCardUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        addCardUseCase = AddCardUseCase(repository)
    }

    @Test
    fun testAddCardDelegatesToRepository() = runTest {
        val card = CreditCard(
            id = "1",
            balance = 1000.0,
            cardNumber = "1234 5678 1234 5678",
            cardHolder = "Johnathan Doe",
            gradientColors = persistentListOf(0xFF000000L, 0xFFFFFFFFL)
        )

        addCardUseCase(card)

        verify(repository).addCard(card)
    }
}
