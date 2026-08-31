package com.hevincj.cashflow.widget

import android.content.Context
import com.hevincj.cashflow.data.local.entity.TransactionEntity
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.models.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class BalanceWidgetTest {

    @Mock
    lateinit var mockContext: Context

    private val testZoneId = ZoneId.of("UTC")
    private val testYearMonth = YearMonth.of(2026, 8)

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(mockContext.packageName).thenReturn("com.hevincj.cashflow")
    }

    private fun findProjectRoot(): File {
        return sequenceOf(
            File("."),
            File(".."),
            File("../..")
        ).map { it.canonicalFile }
            .firstOrNull { File(it, "app/src/main/res").exists() }
            ?: File(".").canonicalFile
    }

    private fun createTransaction(
        id: Int,
        amount: Double,
        type: TransactionType,
        year: Int = 2026,
        month: Int = 8,
        day: Int = 15
    ): TransactionEntity {
        val timestamp = LocalDate.of(year, month, day)
            .atStartOfDay(testZoneId)
            .toInstant()
            .toEpochMilli()

        return TransactionEntity(
            id = id,
            title = "Test Transaction $id",
            amount = amount,
            type = type,
            category = TransactionCategory.OTHERS,
            timestamp = timestamp,
            iconName = "ic_shopping",
            iconBgColor = 0xFF635BFF.toInt(),
            description = null,
            barcode = null,
            productName = null,
            serverId = "server-$id",
            isSynced = true,
            lastModifiedLocal = timestamp,
            recurringExpenseId = null
        )
    }

    @Test
    fun testCalculateMonthlySummary_withMixedTransactions_calculatesCorrectTotals() {
        val transactions = listOf(
            createTransaction(1, 2000.0, TransactionType.INCOME, 2026, 8, 1),
            createTransaction(2, 500.0, TransactionType.INCOME, 2026, 8, 10),
            createTransaction(3, 300.0, TransactionType.EXPENSE, 2026, 8, 15),
            createTransaction(4, 200.0, TransactionType.EXPENSE, 2026, 8, 20),
            // Out-of-month transactions (should be ignored)
            createTransaction(5, 1000.0, TransactionType.INCOME, 2026, 7, 31),
            createTransaction(6, 400.0, TransactionType.EXPENSE, 2026, 9, 1)
        )

        val summary = BalanceWidgetHelper.calculateMonthlySummary(
            transactions = transactions,
            zoneId = testZoneId,
            currentMonth = testYearMonth
        )

        assertEquals(2500.0, summary.income, 0.001)
        assertEquals(500.0, summary.expense, 0.001)
        assertEquals(2000.0, summary.balance, 0.001)
        assertEquals("$2,000.00", summary.formattedBalance)
        assertEquals("+$2,500.00", summary.formattedIncome)
        assertEquals("-$500.00", summary.formattedExpense)
    }

    @Test
    fun testCalculateMonthlySummary_withNegativeBalance_formatsMinusSignCorrectly() {
        val transactions = listOf(
            createTransaction(1, 100.0, TransactionType.INCOME, 2026, 8, 5),
            createTransaction(2, 250.0, TransactionType.EXPENSE, 2026, 8, 10)
        )

        val summary = BalanceWidgetHelper.calculateMonthlySummary(
            transactions = transactions,
            zoneId = testZoneId,
            currentMonth = testYearMonth
        )

        assertEquals(100.0, summary.income, 0.001)
        assertEquals(250.0, summary.expense, 0.001)
        assertEquals(-150.0, summary.balance, 0.001)
        assertEquals("-$150.00", summary.formattedBalance)
        assertEquals("+$100.00", summary.formattedIncome)
        assertEquals("-$250.00", summary.formattedExpense)
    }

    @Test
    fun testCalculateMonthlySummary_withEmptyList_returnsZeroValues() {
        val summary = BalanceWidgetHelper.calculateMonthlySummary(
            transactions = emptyList(),
            zoneId = testZoneId,
            currentMonth = testYearMonth
        )

        assertEquals(0.0, summary.income, 0.001)
        assertEquals(0.0, summary.expense, 0.001)
        assertEquals(0.0, summary.balance, 0.001)
        assertEquals("$0.00", summary.formattedBalance)
        assertEquals("+$0.00", summary.formattedIncome)
        assertEquals("-$0.00", summary.formattedExpense)
    }

    @Test
    fun testFormatBalance_positiveAndNegativeValues() {
        assertEquals("$0.00", BalanceWidgetHelper.formatBalance(0.0))
        assertEquals("$1,234.56", BalanceWidgetHelper.formatBalance(1234.56))
        assertEquals("-$50.00", BalanceWidgetHelper.formatBalance(-50.0))
        assertEquals("-$1,000.50", BalanceWidgetHelper.formatBalance(-1000.50))
    }

    @Test
    fun testQuickAddActionConstant() {
        assertEquals("com.hevincj.cashflow.ACTION_ADD_TRANSACTION", BalanceWidgetHelper.ACTION_ADD_TRANSACTION)
    }

    @Test
    fun testWidgetProviderXmlMetadataExistsAndIsValid() {
        val root = findProjectRoot()
        val widgetInfoXml = File(root, "app/src/main/res/xml/balance_widget_info.xml")
        assertTrue("balance_widget_info.xml must exist", widgetInfoXml.exists())

        val content = widgetInfoXml.readText()
        assertTrue("Must reference initialLayout", content.contains("android:initialLayout=\"@layout/widget_initial_layout\""))
        assertTrue("Must reference previewLayout", content.contains("android:previewLayout=\"@layout/widget_preview\""))
        assertTrue("Must specify updatePeriodMillis", content.contains("android:updatePeriodMillis=\"1800000\""))
    }

    @Test
    fun testWidgetReceiverRegisteredInAndroidManifest() {
        val root = findProjectRoot()
        val manifestXml = File(root, "app/src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml must exist", manifestXml.exists())

        val content = manifestXml.readText()
        assertTrue("Manifest must declare BalanceWidgetReceiver", content.contains("com.hevincj.cashflow.widget.BalanceWidgetReceiver"))
        assertTrue("Manifest must include APPWIDGET_UPDATE intent-filter", content.contains("android.appwidget.action.APPWIDGET_UPDATE"))
    }
}
