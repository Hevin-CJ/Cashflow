package com.hevincj.cashflow.data.repository

import com.hevincj.cashflow.data.local.dao.TransactionDao
import com.hevincj.cashflow.data.local.PendingDeleteManager
import com.hevincj.cashflow.data.mapper.toDomain
import com.hevincj.cashflow.data.mapper.toEntity
import com.hevincj.cashflow.data.remote.api.TransactionApi
import com.hevincj.cashflow.data.worker.TransactionSyncScheduler
import com.hevincj.cashflow.data.worker.TransactionSyncManager
import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao,
    private val api: TransactionApi,
    private val syncScheduler: TransactionSyncScheduler,
    private val pendingDeleteManager: PendingDeleteManager,
    private val syncManager: TransactionSyncManager
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return dao.getAllTransactions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun syncTransactions(limit: Int): String? = withContext(Dispatchers.IO) {
        try {
            // 1. Sync local unsynced transactions inline first
            val currentLocalBeforeFetch = dao.getAllTransactions().first()
            val unsyncedLocal = currentLocalBeforeFetch.filter { !it.isSynced }
            val pendingDeletes = pendingDeleteManager.getPendingDeletions()
            
            unsyncedLocal.forEach { entity ->
                val action = if (entity.serverId != null && entity.serverId in pendingDeletes) "delete" else "upsert"
                val error = syncManager.syncSpecificTransaction(action, entity.id, entity.serverId)
                if (error != null) {
                    return@withContext error
                }
            }

            // 2. Fetch remote transactions
            val response = api.getTransactions(page = 1, limit = limit)
            if (response.isSuccessful) {
                response.body()?.let { dtos ->
                    val currentLocal = dao.getAllTransactions().first()
                    val activePendingDeletes = pendingDeleteManager.getPendingDeletions()
                    
                    val unsyncedServerIds = currentLocal.filter { !it.isSynced && it.serverId != null }
                        .map { it.serverId!! }.toSet()
                    val activeDtos = dtos.filter { it.id !in activePendingDeletes && it.id !in unsyncedServerIds }

                    // Merge: Keep local unsynced, but overwrite synced ones with remote fresh data
                    val syncedLocal = currentLocal.filter { it.isSynced }
                    
                    // Create a map of existing local synced transactions by serverId
                    val localSyncedMap = syncedLocal.filter { it.serverId != null }.associateBy { it.serverId }
                    
                    // Map remote transactions to entities, preserving their existing local primary key IDs
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
                    
                    // Identify local synced transactions that have been deleted from the remote server within the fetched timeframe
                    val remoteServerIds = activeDtos.map { it.id }.toSet()
                    val oldestRemoteTimestamp = activeDtos.minOfOrNull { it.timestamp } ?: 0L
                    val toDelete = syncedLocal.filter { local ->
                        local.serverId != null && 
                        local.serverId !in remoteServerIds && 
                        (local.timestamp >= oldestRemoteTimestamp || activeDtos.size < limit)
                    }
                    
                    // Apply deletions and updates atomically in a single transaction
                    dao.refreshSyncedTransactions(
                        toDelete = toDelete,
                        toInsert = remoteEntities
                    )
                }
                return@withContext null
            } else {
                return@withContext "Failed to sync: ${response.message()} (code ${response.code()})"
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            return@withContext when (e) {
                is java.net.ConnectException -> "Failed to connect to the server."
                is java.net.SocketTimeoutException -> "Connection timed out."
                is java.net.UnknownHostException -> "No internet connection"
                is java.io.EOFException -> "Server connection closed unexpectedly."
                else -> "Server is unreachable. Please try again later."
            }
        }
    }


    override suspend fun insertTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        // 1. Map to entity and mark unsynced initially
        var entity = transaction.toEntity().copy(isSynced = false)
        
        // Match with any existing local transaction by serverId to preserve local primary key id
        if (entity.serverId != null) {
            val existing = dao.getAllTransactions().first().find { it.serverId == entity.serverId }
            if (existing != null) {
                entity = entity.copy(id = existing.id)
            }
        }
        
        val localId = dao.insertTransaction(entity)
        syncScheduler.scheduleUpsertSync(localId.toInt())
    }




    override suspend fun deleteTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        if (transaction.id.isEmpty()) return@withContext
        val isLocalId = transaction.id.all { it.isDigit() }
        
        var serverId: String? = null
        if (isLocalId) {
            val existing = dao.getAllTransactions().first().find { it.id == transaction.id.toInt() }
            serverId = existing?.serverId
            dao.deleteById(transaction.id.toInt())
        } else {
            serverId = transaction.id
            dao.deleteByServerId(transaction.id)
        }

        if (serverId != null) {
            pendingDeleteManager.addPendingDeletion(serverId)
            syncScheduler.scheduleDeleteSync(serverId)
        }
    }
}
