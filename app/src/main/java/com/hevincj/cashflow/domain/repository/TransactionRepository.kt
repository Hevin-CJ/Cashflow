package com.hevincj.cashflow.domain.repository

import com.hevincj.cashflow.domain.models.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    suspend fun insertTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun syncTransactions(limit: Int = 1000): String?
    suspend fun getTransactionById(id: String): Transaction?
    suspend fun getAllTransactionsList(): List<Transaction>
}
