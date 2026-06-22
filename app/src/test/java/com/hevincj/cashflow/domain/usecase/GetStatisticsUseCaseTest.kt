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
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class GetStatisticsUseCaseTest {

    @Mock
    lateinit var repository: TransactionRepository

    private lateinit var getStatisticsUseCase: GetStatisticsUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        getStatisticsUseCase = GetStatisticsUseCase(repository)
    }

    private fun createTimestamp(year: Int, month: Int, day: Int): Long {
        return LocalDate.of(year, month, day)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun createDummyTransaction(
        id: String,
        timestamp: Long,
        amount: Double,
        type: TransactionType
    ): Transaction {
        return Transaction(
            id = id,
            title = "Dummy",
            timestamp = timestamp,
            amount = amount,
            icon = Icons.Rounded.ShoppingBag,
            iconBgColor = Color.Red,
            type = type,
            category = TransactionCategory.GROCERIES,
            description = null,
            isSynced = true
        )
    }

    @Test
    fun testStatisticsFiltersCorrectMonthAndYear() = runTest {
        val targetYear = 2026
        val targetMonth = 6

        val transactions = listOf(
            createDummyTransaction("1", createTimestamp(2026, 6, 15), 100.0, TransactionType.INCOME),
            createDummyTransaction("2", createTimestamp(2026, 5, 15), 200.0, TransactionType.INCOME),
            createDummyTransaction("3", createTimestamp(2025, 6, 15), 300.0, TransactionType.INCOME)
        )

        whenever(repository.getAllTransactions()).thenReturn(flowOf(transactions))

        val stats = getStatisticsUseCase(targetYear, targetMonth).first()

        assertEquals(100.0, stats.totalIncome, 0.01)
        assertEquals(1, stats.recentTransactions.size)
        assertEquals("1", stats.recentTransactions[0].id)
    }

    @Test
    fun testStatisticsGroupsByWeekCorrectly() = runTest {
        val targetYear = 2026
        val targetMonth = 6

        // Week 0: Day 1-7
        // Week 1: Day 8-14
        // Week 2: Day 15-21
        // Week 3: Day 22-30
        val transactions = listOf(
            createDummyTransaction("w0", createTimestamp(2026, 6, 5), 10.0, TransactionType.INCOME),
            createDummyTransaction("w1", createTimestamp(2026, 6, 12), -20.0, TransactionType.EXPENSE),
            createDummyTransaction("w2", createTimestamp(2026, 6, 18), 30.0, TransactionType.INCOME),
            createDummyTransaction("w3", createTimestamp(2026, 6, 25), -40.0, TransactionType.EXPENSE)
        )

        whenever(repository.getAllTransactions()).thenReturn(flowOf(transactions))

        val stats = getStatisticsUseCase(targetYear, targetMonth).first()

        // Verify weekly incomes
        assertEquals(10f, stats.weeklyIncome[0])
        assertEquals(0f, stats.weeklyIncome[1])
        assertEquals(30f, stats.weeklyIncome[2])
        assertEquals(0f, stats.weeklyIncome[3])

        // Verify weekly expenses
        assertEquals(0f, stats.weeklyExpenses[0])
        assertEquals(20f, stats.weeklyExpenses[1])
        assertEquals(0f, stats.weeklyExpenses[2])
        assertEquals(40f, stats.weeklyExpenses[3])
    }

    @Test
    fun testStatisticsSortsChronologicallyDescending() = runTest {
        val targetYear = 2026
        val targetMonth = 6

        val transactions = listOf(
            createDummyTransaction("oldest", createTimestamp(2026, 6, 1), 50.0, TransactionType.INCOME),
            createDummyTransaction("newest", createTimestamp(2026, 6, 30), 100.0, TransactionType.INCOME),
            createDummyTransaction("middle", createTimestamp(2026, 6, 15), 75.0, TransactionType.INCOME)
        )

        whenever(repository.getAllTransactions()).thenReturn(flowOf(transactions))

        val stats = getStatisticsUseCase(targetYear, targetMonth).first()

        assertEquals(3, stats.recentTransactions.size)
        assertEquals("newest", stats.recentTransactions[0].id)
        assertEquals("middle", stats.recentTransactions[1].id)
        assertEquals("oldest", stats.recentTransactions[2].id)
    }

    @Test
    fun testStatisticsCalculatesCorrectIncomeAndExpenseTotals() = runTest {
        val targetYear = 2026
        val targetMonth = 6

        val transactions = listOf(
            createDummyTransaction("1", createTimestamp(2026, 6, 5), 100.0, TransactionType.INCOME),
            createDummyTransaction("2", createTimestamp(2026, 6, 10), -50.0, TransactionType.EXPENSE),
            // Amount can be stored negative or positive, use abs() in code
            createDummyTransaction("3", createTimestamp(2026, 6, 15), -200.0, TransactionType.EXPENSE),
            createDummyTransaction("4", createTimestamp(2026, 6, 20), 150.0, TransactionType.INCOME)
        )

        whenever(repository.getAllTransactions()).thenReturn(flowOf(transactions))

        val stats = getStatisticsUseCase(targetYear, targetMonth).first()

        assertEquals(250.0, stats.totalIncome, 0.01)
        assertEquals(250.0, stats.totalExpenses, 0.01)
    }
}
