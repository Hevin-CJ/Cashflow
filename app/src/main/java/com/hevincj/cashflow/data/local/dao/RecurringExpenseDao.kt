package com.hevincj.cashflow.data.local.dao

import androidx.room.*
import com.hevincj.cashflow.data.local.entity.RecurringExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseDao {
    @Query("SELECT * FROM recurring_expenses ORDER BY nextDueDate ASC")
    fun getAllRecurringExpenses(): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses")
    suspend fun getActiveRecurringExpensesList(): List<RecurringExpenseEntity>

    @Query("SELECT * FROM recurring_expenses WHERE id = :id LIMIT 1")
    suspend fun getRecurringExpenseById(id: Int): RecurringExpenseEntity?

    @Query("SELECT * FROM recurring_expenses WHERE serverId = :serverId LIMIT 1")
    suspend fun getRecurringExpenseByServerId(serverId: String): RecurringExpenseEntity?

    @Query("SELECT * FROM recurring_expenses WHERE isSynced = 0")
    suspend fun getUnsyncedRecurringExpenses(): List<RecurringExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringExpense(recurringExpense: RecurringExpenseEntity): Long

    @Update
    suspend fun updateRecurringExpense(recurringExpense: RecurringExpenseEntity)

    @Query("UPDATE recurring_expenses SET nextDueDate = :nextDueDate, lastProcessedDate = :lastProcessedDate WHERE id = :id")
    suspend fun updateBillingPointersById(id: Int, nextDueDate: Long, lastProcessedDate: Long?)

    @Query("UPDATE recurring_expenses SET nextDueDate = :nextDueDate, lastProcessedDate = :lastProcessedDate WHERE serverId = :serverId")
    suspend fun updateBillingPointersByServerId(serverId: String, nextDueDate: Long, lastProcessedDate: Long?)

    @Query("DELETE FROM recurring_expenses WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM recurring_expenses WHERE serverId = :serverId")
    suspend fun deleteByServerId(serverId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringExpenses(expenses: List<RecurringExpenseEntity>)

    @Delete
    suspend fun deleteRecurringExpenses(expenses: List<RecurringExpenseEntity>)

    @Transaction
    suspend fun refreshSyncedRecurringExpenses(toDelete: List<RecurringExpenseEntity>, toInsert: List<RecurringExpenseEntity>) {
        deleteRecurringExpenses(toDelete)
        insertRecurringExpenses(toInsert)
    }
}
