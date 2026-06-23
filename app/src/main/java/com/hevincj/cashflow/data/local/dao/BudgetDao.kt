package com.hevincj.cashflow.data.local.dao

import androidx.room.*
import com.hevincj.cashflow.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year")
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsList(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE category = :category AND month = :month AND year = :year")
    suspend fun getBudgetByCategoryAndMonth(category: String, month: Int, year: Int): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE isSynced = 0")
    suspend fun getUnsyncedBudgets(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudgetById(id: Int): BudgetEntity?

    @Query("UPDATE budgets SET serverId = :serverId, isSynced = :isSynced WHERE id = :id")
    suspend fun updateBudgetSyncStatus(id: Int, serverId: String, isSynced: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets WHERE category = :category AND month = :month AND year = :year")
    suspend fun deleteBudget(category: String, month: Int, year: Int)

    @Delete
    suspend fun deleteBudgets(budgets: List<BudgetEntity>)

    @Transaction
    suspend fun refreshSyncedBudgets(toDelete: List<BudgetEntity>, toInsert: List<BudgetEntity>) {
        deleteBudgets(toDelete)
        insertBudgets(toInsert)
    }
}
