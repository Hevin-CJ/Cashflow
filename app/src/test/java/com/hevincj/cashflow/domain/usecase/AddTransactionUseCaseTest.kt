package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.domain.repository.TransactionRepository
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ShoppingBag
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionUseCaseTest {

    @Mock
    lateinit var repository: TransactionRepository

    private lateinit var addTransactionUseCase: AddTransactionUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        addTransactionUseCase = AddTransactionUseCase(repository)
    }

    @Test
    fun testAddTransactionDelegatesToRepository() = runTest {
        val transaction = Transaction(
            id = "1",
            title = "Test",
            timestamp = 1000L,
            amount = 100.0,
            icon = Icons.Rounded.ShoppingBag,
            iconBgColor = Color.Blue,
            type = TransactionType.INCOME,
            category = TransactionCategory.OTHERS,
            description = "desc",
            isSynced = false
        )

        addTransactionUseCase(transaction)

        verify(repository).insertTransaction(transaction)
    }
}
