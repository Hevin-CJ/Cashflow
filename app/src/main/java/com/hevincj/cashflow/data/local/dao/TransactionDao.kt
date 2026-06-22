package com.hevincj.cashflow.data.local.dao

import androidx.room.*
import com.hevincj.cashflow.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY id DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM transactions WHERE serverId = :serverId")
    suspend fun deleteByServerId(serverId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Delete
    suspend fun deleteTransactions(transactions: List<TransactionEntity>)

    @Transaction
    suspend fun refreshSyncedTransactions(toDelete: List<TransactionEntity>, toInsert: List<TransactionEntity>) {
        deleteTransactions(toDelete)
        insertTransactions(toInsert)
    }
}

