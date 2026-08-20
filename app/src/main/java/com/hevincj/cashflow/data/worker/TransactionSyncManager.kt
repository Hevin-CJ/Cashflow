package com.hevincj.cashflow.data.worker

import com.hevincj.cashflow.data.local.dao.TransactionDao
import com.hevincj.cashflow.data.local.dao.RecurringExpenseDao
import com.hevincj.cashflow.data.local.entity.TransactionEntity
import com.hevincj.cashflow.data.local.PendingDeleteManager
import com.hevincj.cashflow.data.remote.api.TransactionApi
import com.hevincj.cashflow.data.remote.models.TransactionRequestDto
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class TransactionSyncManager @Inject constructor(
    private val dao: TransactionDao,
    private val recurringExpenseDao: RecurringExpenseDao,
    private val recurringExpenseSyncManager: javax.inject.Provider<RecurringExpenseSyncManager>,
    private val api: TransactionApi,
    private val pendingDeleteManager: PendingDeleteManager
) {
    private val transactionLocks = ConcurrentHashMap<Int, Mutex>()
    private val deleteLocks = ConcurrentHashMap<String, Mutex>()

    suspend fun syncSpecificTransaction(action: String, localId: Int, serverId: String?): String? {
        try {
            if (action == "upsert" && localId != -1) {
                val lock = transactionLocks.computeIfAbsent(localId) { Mutex() }
                return lock.withLock {
                    val entity = dao.getTransactionById(localId) ?: return null // Deleted locally, skip
                    if (entity.isSynced) return null // Already synced

                    val resolvedRecurringId = entity.recurringExpenseId?.let { id ->
                        if (id.all { it.isDigit() }) {
                            val parent = recurringExpenseDao.getRecurringExpenseById(id.toInt())
                            if (parent != null) {
                                if (parent.serverId == null) {
                                    // Trigger parent sync synchronously
                                    val syncSuccess = recurringExpenseSyncManager.get()
                                        .syncSpecificRecurringExpense("upsert", id.toInt(), null)
                                    if (syncSuccess) {
                                        // Fetch again to get the serverId
                                        recurringExpenseDao.getRecurringExpenseById(id.toInt())?.serverId ?: id
                                    } else {
                                        id
                                    }
                                } else {
                                    parent.serverId
                                }
                            } else {
                                id
                            }
                        } else {
                            id
                        }
                    }

                    val requestDto = TransactionRequestDto(
                        amount = kotlin.math.abs(entity.amount),
                        type = entity.type.name,
                        category = entity.category.name,
                        description = entity.title, // Preserves local title as description on server
                        barcode = entity.barcode,
                        productName = entity.productName,
                        recurringExpenseId = resolvedRecurringId,
                        timestamp = entity.timestamp
                    )

                    if (entity.serverId != null) {
                        val response = api.updateTransaction(entity.serverId, requestDto)
                        if (response.isSuccessful) {
                            dao.insertTransaction(
                                entity.copy(
                                    isSynced = true,
                                    lastModifiedLocal = System.currentTimeMillis()
                                )
                            )
                            return null
                        } else if (response.code() == 404) {
                            // Fallback to creation if server record was wiped
                            val createResponse = api.createTransaction(requestDto)
                            if (createResponse.isSuccessful) {
                                createResponse.body()?.let { remoteDto ->
                                    dao.insertTransaction(
                                        entity.copy(
                                            isSynced = true,
                                            serverId = remoteDto.id,
                                            lastModifiedLocal = System.currentTimeMillis()
                                        )
                                    )
                                }
                                return null
                            } else {
                                return "Failed to sync transaction (create fallback after 404): ${createResponse.message()} (code ${createResponse.code()})"
                            }
                        } else {
                            return "Failed to update transaction: ${response.message()} (code ${response.code()})"
                        }
                    } else {
                        val response = api.createTransaction(requestDto)
                        if (response.isSuccessful) {
                            response.body()?.let { remoteDto ->
                                dao.insertTransaction(
                                    entity.copy(
                                        isSynced = true,
                                        serverId = remoteDto.id,
                                        lastModifiedLocal = System.currentTimeMillis()
                                    )
                                )
                            }
                            return null
                        } else {
                            val errorMsg = "Failed to create transaction: ${response.message()} (code ${response.code()})"
                            com.hevincj.cashflow.utils.CrashLogger.w("TransactionSyncManager", errorMsg)
                            return errorMsg
                        }
                    }
                }
            } else if (action == "delete" && serverId != null) {
                // Re-added the missing deletion synchronization flow
                val lock = deleteLocks.computeIfAbsent(serverId) { Mutex() }
                return lock.withLock {
                    val response = api.deleteTransaction(serverId)
                    if (response.isSuccessful || response.code() == 404) {
                        pendingDeleteManager.removePendingDeletion(serverId)
                        return null
                    }
                    val errorMsg = "Failed to delete transaction: ${response.message()} (code ${response.code()})"
                    com.hevincj.cashflow.utils.CrashLogger.w("TransactionSyncManager", errorMsg)
                    return errorMsg
                }
            }
            return null
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            com.hevincj.cashflow.utils.CrashLogger.w("TransactionSyncManager", "Exception during specific transaction sync: ${e.message}", e)
            return e.message ?: "Unknown sync error"
        }
    }
}