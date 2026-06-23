package com.hevincj.cashflow.data.repository

import com.hevincj.cashflow.data.local.dao.RecurringExpenseDao
import com.hevincj.cashflow.data.local.PendingDeleteManager
import com.hevincj.cashflow.data.mapper.toDomain
import com.hevincj.cashflow.data.mapper.toEntity
import com.hevincj.cashflow.data.remote.api.RecurringExpenseApi
import com.hevincj.cashflow.data.worker.RecurringExpenseSyncScheduler
import com.hevincj.cashflow.data.worker.RecurringExpenseSyncManager
import com.hevincj.cashflow.domain.models.RecurringExpense
import com.hevincj.cashflow.domain.repository.RecurringExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RecurringExpenseRepositoryImpl @Inject constructor(
    private val dao: RecurringExpenseDao,
    private val api: RecurringExpenseApi,
    private val syncScheduler: RecurringExpenseSyncScheduler,
    private val pendingDeleteManager: PendingDeleteManager,
    private val syncManager: RecurringExpenseSyncManager
) : RecurringExpenseRepository {

    override fun getAllRecurringExpenses(): Flow<List<RecurringExpense>> {
        return dao.getAllRecurringExpenses().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getActiveRecurringExpenses(): List<RecurringExpense> = withContext(Dispatchers.IO) {
        dao.getActiveRecurringExpensesList().map { it.toDomain() }
    }

    override suspend fun insertRecurringExpense(recurringExpense: RecurringExpense) = withContext(Dispatchers.IO) {
        var entity = recurringExpense.toEntity().copy(isSynced = false)

        if (entity.serverId != null) {
            val existing = dao.getActiveRecurringExpensesList().find { it.serverId == entity.serverId }
            if (existing != null) {
                entity = entity.copy(id = existing.id)
            }
        }

        val localId = dao.insertRecurringExpense(entity)
        syncScheduler.scheduleUpsertSync(localId.toInt())
    }

    override suspend fun updateRecurringExpense(recurringExpense: RecurringExpense) = withContext(Dispatchers.IO) {
        val entity = recurringExpense.toEntity().copy(isSynced = false)
        dao.updateRecurringExpense(entity)
        
        val isLocalId = recurringExpense.id.all { it.isDigit() }
        if (isLocalId && recurringExpense.id.isNotEmpty()) {
            syncScheduler.scheduleUpsertSync(recurringExpense.id.toInt())
        } else if (recurringExpense.serverId != null) {
            val existingLocal = dao.getActiveRecurringExpensesList().find { it.serverId == recurringExpense.serverId }
            if (existingLocal != null) {
                syncScheduler.scheduleUpsertSync(existingLocal.id)
            }
        }
    }

    override suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense) = withContext(Dispatchers.IO) {
        if (recurringExpense.id.isEmpty()) return@withContext
        val isLocalId = recurringExpense.id.all { it.isDigit() }

        var serverId: String? = null
        if (isLocalId) {
            val existing = dao.getActiveRecurringExpensesList().find { it.id == recurringExpense.id.toInt() }
            serverId = existing?.serverId
            dao.deleteById(recurringExpense.id.toInt())
        } else {
            serverId = recurringExpense.id
            dao.deleteByServerId(recurringExpense.id)
        }

        if (serverId != null) {
            pendingDeleteManager.addPendingRecurringDeletion(serverId)
            syncScheduler.scheduleDeleteSync(serverId)
        }
    }

    override suspend fun syncRecurringExpenses(): String? = withContext(Dispatchers.IO) {
        try {
            val currentLocalBeforeFetch = dao.getActiveRecurringExpensesList()
            val unsyncedLocal = currentLocalBeforeFetch.filter { !it.isSynced }
            val pendingDeletes = pendingDeleteManager.getPendingRecurringDeletions()

            unsyncedLocal.forEach { entity ->
                val action = if (entity.serverId != null && entity.serverId in pendingDeletes) "delete" else "upsert"
                syncManager.syncSpecificRecurringExpense(action, entity.id, entity.serverId)
            }

            val response = api.getRecurringExpenses()
            if (response.isSuccessful) {
                response.body()?.let { dtos ->
                    val currentLocal = dao.getActiveRecurringExpensesList()
                    val activePendingDeletes = pendingDeleteManager.getPendingRecurringDeletions()

                    val unsyncedServerIds = currentLocal.filter { !it.isSynced && it.serverId != null }
                        .map { it.serverId!! }.toSet()
                    val activeDtos = dtos.filter { it.id !in activePendingDeletes && it.id !in unsyncedServerIds }

                    val syncedLocal = currentLocal.filter { it.isSynced }
                    val localSyncedMap = syncedLocal.filter { it.serverId != null }.associateBy { it.serverId }

                    val remoteEntities = activeDtos.map { dto ->
                        val domain = dto.toDomain()
                        val entity = domain.toEntity()
                        val existing = localSyncedMap[dto.id]
                        if (existing != null) {
                            entity.copy(id = existing.id)
                        } else {
                            entity
                        }
                    }

                    val remoteServerIds = activeDtos.map { it.id }.toSet()
                    val toDelete = syncedLocal.filter { local ->
                        local.serverId != null && local.serverId !in remoteServerIds
                    }

                    dao.refreshSyncedRecurringExpenses(
                        toDelete = toDelete,
                        toInsert = remoteEntities
                    )
                }
                return@withContext null
            } else {
                return@withContext "Failed to sync subscriptions: ${response.message()} (code ${response.code()})"
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            return@withContext "Failed to connect to the server."
        }
    }
}
