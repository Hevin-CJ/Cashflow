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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.hevincj.cashflow.widget.BalanceWidget
import androidx.glance.appwidget.updateAll

class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao,
    private val api: TransactionApi,
    private val syncScheduler: TransactionSyncScheduler,
    private val pendingDeleteManager: PendingDeleteManager,
    private val syncManager: TransactionSyncManager,
    @param:ApplicationContext private val context: Context
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return dao.getAllTransactions().map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getTransactionById(id: String): Transaction? = withContext(Dispatchers.IO) {
        if (id.isEmpty()) return@withContext null
        val isLocalId = id.all { it.isDigit() }
        val entity = if (isLocalId) {
            dao.getTransactionById(id.toInt())
        } else {
            dao.getTransactionByServerId(id)
        }
        entity?.toDomain()
    }

    override suspend fun getAllTransactionsList(): List<Transaction> = withContext(Dispatchers.IO) {
        dao.getAllTransactionsList().map { it.toDomain() }
    }

    override suspend fun syncTransactions(limit: Int): String? = withContext(Dispatchers.IO) {
        try {
            // 1. Sync local unsynced transactions inline first
            val unsyncedLocal = dao.getUnsyncedTransactions()
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
                    val currentLocal = dao.getAllTransactionsList()
                    val activePendingDeletes = pendingDeleteManager.getPendingDeletions()

                    val currentTime = System.currentTimeMillis()
                    val recentlyModifiedServerIds = currentLocal.filter {
                        it.serverId != null && (currentTime - it.lastModifiedLocal < 60000)
                    }.map { it.serverId!! }.toSet()

                    val unsyncedServerIds = currentLocal.filter { !it.isSynced && it.serverId != null }
                        .map { it.serverId!! }.toSet()

                    val activeDtos = dtos.filter {
                        it.id !in activePendingDeletes &&
                                it.id !in unsyncedServerIds &&
                                it.id !in recentlyModifiedServerIds
                    }

                    val syncedLocal = currentLocal.filter { it.isSynced }

                    val localTransactionsMap = currentLocal.filter { it.serverId != null }.associateBy { it.serverId }

                    val remoteEntities = activeDtos.map { dto ->
                        val domain = dto.toDomain()
                        val entity = domain.toEntity().copy(recurringExpenseId = dto.recurringExpenseId)
                        val existing = localTransactionsMap[dto.id]
                        if (existing != null) {
                            entity.copy(
                                id = existing.id,
                                lastModifiedLocal = existing.lastModifiedLocal,
                                recurringExpenseId = existing.recurringExpenseId ?: entity.recurringExpenseId
                            )
                        } else {
                            entity
                        }
                    }

                    val remoteServerIds = activeDtos.map { it.id }.toSet()
                    val oldestRemoteTimestamp = activeDtos.minOfOrNull { it.timestamp } ?: 0L

                    val toDelete = syncedLocal.filter { local ->
                        local.serverId != null &&
                                local.serverId !in remoteServerIds &&
                                local.serverId !in recentlyModifiedServerIds &&
                                (local.timestamp >= oldestRemoteTimestamp || dtos.size < limit)
                    }

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
        // Preserve explicit title if present, otherwise fallback to description or category displayName
        val resolvedTitle = transaction.title.ifBlank {
            if (transaction.description.isNullOrBlank()) {
                transaction.category.displayName
            } else {
                transaction.description
            }
        }

        var entity = transaction.toEntity().copy(
            title = resolvedTitle,
            isSynced = false,
            lastModifiedLocal = System.currentTimeMillis()
        )

        if (entity.serverId != null) {
            val existing = dao.getTransactionByServerId(entity.serverId)
            if (existing != null) {
                entity = entity.copy(
                    id = existing.id,
                    recurringExpenseId = existing.recurringExpenseId ?: entity.recurringExpenseId
                )
            }
        }

        val localId = dao.insertTransaction(entity)
        syncScheduler.scheduleUpsertSync(localId.toInt())
        try {
            BalanceWidget().updateAll(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun updateTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        val isLocalId = transaction.id.all { it.isDigit() }
        val existing = if (isLocalId) {
            dao.getTransactionById(transaction.id.toInt())
        } else {
            dao.getTransactionByServerId(transaction.id)
        }

        // FIX: Re-evaluate title during updates. If description is blank, snap title to the new category name.
        val resolvedTitle = if (transaction.description.isNullOrBlank()) {
            transaction.category.displayName
        } else {
            transaction.description
        }

        var entity = transaction.toEntity().copy(
            title = resolvedTitle,
            isSynced = false,
            lastModifiedLocal = System.currentTimeMillis()
        )

        if (existing != null) {
            entity = entity.copy(
                id = existing.id,
                serverId = existing.serverId ?: entity.serverId,
                recurringExpenseId = existing.recurringExpenseId ?: entity.recurringExpenseId
            )
        } else if (isLocalId) {
            entity = entity.copy(id = transaction.id.toInt())
        }

        val localId = dao.insertTransaction(entity)
        syncScheduler.scheduleUpsertSync(localId.toInt())
        try {
            BalanceWidget().updateAll(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun deleteTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        if (transaction.id.isEmpty()) return@withContext
        val isLocalId = transaction.id.all { it.isDigit() }

        var serverId: String? = null
        if (isLocalId) {
            val idInt = transaction.id.toInt()
            val existing = dao.getTransactionById(idInt)
            serverId = existing?.serverId
            dao.deleteById(idInt)
        } else {
            serverId = transaction.id
            dao.deleteByServerId(transaction.id)
        }

        if (serverId != null) {
            pendingDeleteManager.addPendingDeletion(serverId)
            syncScheduler.scheduleDeleteSync(serverId)
        }
        try {
            BalanceWidget().updateAll(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}