package com.hevincj.cashflow.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.actionParametersOf
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.hevincj.cashflow.MainActivity
import com.hevincj.cashflow.data.local.dao.TransactionDao
import com.hevincj.cashflow.domain.models.TransactionType
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs

class BalanceWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun getTransactionDao(): TransactionDao
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var balance = 0.0
        var income = 0.0
        var expense = 0.0

        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetEntryPoint::class.java
            )
            val transactionDao = entryPoint.getTransactionDao()

            // Fetch transactions from the database
            val transactions = transactionDao.getAllTransactionsList()

            val zoneId = ZoneId.systemDefault()
            val currentMonth = YearMonth.now()
            val filtered = transactions.filter { tx ->
                val txYearMonth = YearMonth.from(
                    Instant.ofEpochMilli(tx.timestamp)
                        .atZone(zoneId)
                        .toLocalDate()
                )
                txYearMonth == currentMonth
            }

            income = filtered.filter { it.type == TransactionType.INCOME }.sumOf { abs(it.amount) }
            expense = filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { abs(it.amount) }
            balance = income - expense
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val balanceText = String.format("$%,.2f", balance)
        val incomeText = String.format("+$%,.2f", income)
        val expenseText = String.format("-$%,.2f", expense)

        // Launch MainActivity with explicit Intent on click
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.hevincj.cashflow.ACTION_ADD_TRANSACTION"
        }
        val quickAddAction = actionStartActivity(intent)

        provideContent {
            GlanceTheme {
                WidgetContent(
                    balance = balanceText,
                    income = incomeText,
                    expense = expenseText,
                    quickAddAction = quickAddAction
                )
            }
        }
    }

    @Composable
    private fun WidgetContent(
        balance: String,
        income: String,
        expense: String,
        quickAddAction: androidx.glance.action.Action
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF0F0E17)))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "THIS MONTH'S BALANCE",
                style = TextStyle(
                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF94A3B8)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            Text(
                text = balance,
                style = TextStyle(
                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFFFFFFF)),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Income / Expense row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = income,
                    style = TextStyle(
                        color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF10B981)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.width(12.dp))
                Text(
                    text = expense,
                    style = TextStyle(
                        color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFEF4444)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(16.dp))

            // Quick Add Button
            Button(
                text = "Quick Add",
                onClick = quickAddAction,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = ColorProvider(androidx.compose.ui.graphics.Color(0xFF635BFF)),
                    contentColor = ColorProvider(androidx.compose.ui.graphics.Color(0xFFFFFFFF))
                ),
                modifier = GlanceModifier.fillMaxWidth().height(40.dp)
            )
        }
    }
}
