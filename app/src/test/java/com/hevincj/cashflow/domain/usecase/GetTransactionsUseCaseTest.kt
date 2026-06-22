package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.domain.repository.TransactionRepository
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ShoppingBag
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
class GetTransactionsUseCaseTest {

    @Mock
    lateinit var repository: TransactionRepository

    private lateinit var getTransactionsUseCase: GetTransactionsUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        getTransactionsUseCase = GetTransactionsUseCase(repository)
    }

    @Test
    fun testGetTransactionsDelegatesToRepository() = runTest {
        val sampleTransactions = listOf(
            Transaction(
                id = "1",
                title = "Test",
                timestamp = 1000L,
                amount = 100.0,
                icon = Icons.Rounded.ShoppingBag,
                iconBgColor = Color.Blue,
                type = TransactionType.INCOME,
                category = TransactionCategory.OTHERS,
                description = "desc",
                isSynced = true
            )
        )

        whenever(repository.getAllTransactions()).thenReturn(flowOf(sampleTransactions))

        val result = getTransactionsUseCase().first()

        assertEquals(sampleTransactions, result)
    }
}
