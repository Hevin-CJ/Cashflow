package com.hevincj.cashflow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hevincj.cashflow.data.local.dao.TransactionDao
import com.hevincj.cashflow.data.local.entity.TransactionEntity

import com.hevincj.cashflow.data.local.dao.CreditCardDao
import com.hevincj.cashflow.data.local.entity.CreditCardEntity

import com.hevincj.cashflow.data.local.dao.BudgetDao
import com.hevincj.cashflow.data.local.entity.BudgetEntity

import androidx.room.TypeConverters
import com.hevincj.cashflow.data.local.dao.RecurringExpenseDao
import com.hevincj.cashflow.data.local.entity.RecurringExpenseEntity

@Database(
    entities = [TransactionEntity::class, CreditCardEntity::class, BudgetEntity::class, RecurringExpenseEntity::class],
    version = 12,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CashFlowDatabase : RoomDatabase() {
    abstract val transactionDao: TransactionDao
    abstract val creditCardDao: CreditCardDao
    abstract val budgetDao: BudgetDao
    abstract val recurringExpenseDao: RecurringExpenseDao

    companion object {
        const val DATABASE_NAME = "cashflow_db"
    }
}
