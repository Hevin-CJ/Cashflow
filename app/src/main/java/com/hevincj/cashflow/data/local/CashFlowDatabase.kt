package com.hevincj.cashflow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hevincj.cashflow.data.local.dao.TransactionDao
import com.hevincj.cashflow.data.local.entity.TransactionEntity

import com.hevincj.cashflow.data.local.dao.CreditCardDao
import com.hevincj.cashflow.data.local.entity.CreditCardEntity

import com.hevincj.cashflow.data.local.dao.BudgetDao
import com.hevincj.cashflow.data.local.entity.BudgetEntity

@Database(
    entities = [TransactionEntity::class, CreditCardEntity::class, BudgetEntity::class],
    version = 7,
    exportSchema = false
)
abstract class CashFlowDatabase : RoomDatabase() {
    abstract val transactionDao: TransactionDao
    abstract val creditCardDao: CreditCardDao
    abstract val budgetDao: BudgetDao

    companion object {
        const val DATABASE_NAME = "cashflow_db"
    }
}
