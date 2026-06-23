package com.hevincj.cashflow.data.repository

import com.hevincj.cashflow.data.local.dao.BudgetDao
import com.hevincj.cashflow.data.local.PendingDeleteManager
import com.hevincj.cashflow.data.mapper.toDomain
import com.hevincj.cashflow.data.mapper.toEntity
import com.hevincj.cashflow.data.remote.api.BudgetApi
import com.hevincj.cashflow.data.worker.BudgetSyncScheduler
import com.hevincj.cashflow.data.worker.BudgetSyncManager
import com.hevincj.cashflow.domain.models.Budget
import com.hevincj.cashflow.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val dao: BudgetDao,
    private val api: BudgetApi,
    private val syncScheduler: BudgetSyncScheduler,
    private val pendingDeleteManager: PendingDeleteManager,
    private val syncManager: BudgetSyncManager
) : BudgetRepository {

    override fun getBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>> {
        return dao.getBudgetsForMonth(month, year).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun setBudget(budget: Budget) = withContext(Dispatchers.IO) {
        val existing = dao.getBudgetByCategoryAndMonth(budget.category.name, budget.month, budget.year)
        val entityToInsert = budget.toEntity().copy(
            id = existing?.id ?: 0,
            serverId = existing?.serverId,
            isSynced = false
        )
        dao.insertBudget(entityToInsert)
        val finalId = if (entityToInsert.id != 0) {
            entityToInsert.id
        } else {
            dao.getBudgetByCategoryAndMonth(budget.category.name, budget.month, budget.year)?.id ?: 0
        }
        if (finalId != 0) {
            syncScheduler.scheduleUpsertSync(finalId)
        }
    }

    override suspend fun deleteBudget(category: String, month: Int, year: Int) = withContext(Dispatchers.IO) {
        val existing = dao.getBudgetByCategoryAndMonth(category, month, year)
        if (existing != null) {
            dao.deleteBudget(category, month, year)
            if (existing.serverId != null) {
                pendingDeleteManager.addPendingBudgetDeletion(existing.serverId)
                syncScheduler.scheduleDeleteSync(category, month, year, existing.serverId)
            }
        }
    }

    override suspend fun syncBudgets(): String? = withContext(Dispatchers.IO) {
        try {
            val currentLocalBeforeFetch = dao.getAllBudgetsList()
            val unsyncedLocal = currentLocalBeforeFetch.filter { !it.isSynced }
            val pendingDeletes = pendingDeleteManager.getPendingBudgetDeletions()

            // 1. Sync local offline changes first
            unsyncedLocal.forEach { entity ->
                val action = if (entity.serverId != null && entity.serverId in pendingDeletes) "delete" else "upsert"
                val error = syncManager.syncSpecificBudget(action, entity.id, entity.serverId)
                if (error != null) {
                    return@withContext error
                }
            }

            // 2. Fetch remote budgets
            val response = api.getBudgets()
            if (response.isSuccessful) {
                response.body()?.let { dtos ->
                    val currentLocal = dao.getAllBudgetsList()
                    val activePendingDeletes = pendingDeleteManager.getPendingBudgetDeletions()

                    // Filter out remote budgets that have pending local deletions or are currently unsynced
                    val unsyncedServerIds = currentLocal.filter { !it.isSynced && it.serverId != null }
                        .map { it.serverId!! }.toSet()
                    val activeDtos = dtos.filter { it.id !in activePendingDeletes && it.id !in unsyncedServerIds }

                    val syncedLocal = currentLocal.filter { it.isSynced }
                    val localSyncedMap = syncedLocal.filter { it.serverId != null }.associateBy { it.serverId }

                    // Construct list of remote budget entities to merge
                    val remoteEntities = activeDtos.map { dto ->
                        val domain = dto.toDomain()
                        val entity = domain.toEntity()
                        val existing = localSyncedMap[dto.id]
                        if (existing != null) {
                            entity.copy(id = existing.id)
                        } else {
                            // If local un-synced entity matches unique constraint, preserve its ID to avoid duplicates
                            val matchedLocal = currentLocal.find { 
                                it.category == entity.category && 
                                it.month == entity.month && 
                                it.year == entity.year 
                            }
                            if (matchedLocal != null) {
                                entity.copy(id = matchedLocal.id)
                            } else {
                                entity
                            }
                        }
                    }

                    val remoteServerIds = activeDtos.map { it.id }.toSet()
                    val toDelete = syncedLocal.filter { local ->
                        local.serverId != null && local.serverId !in remoteServerIds
                    }

                    dao.refreshSyncedBudgets(
                        toDelete = toDelete,
                        toInsert = remoteEntities
                    )
                }
                return@withContext null
            } else {
                return@withContext "Failed to sync budgets: ${response.message()} (code ${response.code()})"
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            return@withContext "Failed to connect to the server."
        }
    }
}
