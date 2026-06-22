package com.hevincj.cashflow.data.worker

import com.hevincj.cashflow.data.local.dao.TransactionDao
import com.hevincj.cashflow.data.local.entity.TransactionEntity
import com.hevincj.cashflow.data.local.PendingDeleteManager
import com.hevincj.cashflow.data.remote.api.TransactionApi
import com.hevincj.cashflow.data.remote.models.TransactionRequestDto
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class TransactionSyncManager @Inject constructor(
    private val dao: TransactionDao,
    private val api: TransactionApi,
    private val pendingDeleteManager: PendingDeleteManager
) {
    private val transactionLocks = ConcurrentHashMap<Int, Mutex>()
    private val deleteLocks = ConcurrentHashMap<String, Mutex>()

    suspend fun syncSpecificTransaction(action: String, localId: Int, serverId: String?): Boolean {
        try {
            if (action == "upsert" && localId != -1) {
                val lock = transactionLocks.computeIfAbsent(localId) { Mutex() }
                return lock.withLock {
                    val entity = dao.getAllTransactions().first().find { it.id == localId } ?: return true // Deleted locally, no need to upload
                    if (entity.isSynced) return true // Already synced

                    val requestDto = TransactionRequestDto(
                        amount = kotlin.math.abs(entity.amount),
                        type = entity.type,
                        category = entity.category,
                        description = entity.description,
                        barcode = entity.barcode,
                        productName = entity.productName
                    )

                    if (entity.serverId != null) {
                        val response = api.updateTransaction(entity.serverId, requestDto)
                        if (response.isSuccessful) {
                            dao.insertTransaction(entity.copy(isSynced = true))
                            return true
                        }
                    } else {
                        val response = api.createTransaction(requestDto)
                        if (response.isSuccessful) {
                            response.body()?.let { remoteDto ->
                                dao.insertTransaction(
                                    entity.copy(
                                        isSynced = true,
                                        serverId = remoteDto.id
                                    )
                                )
                            }
                            return true
                        }
                    }
                    false
                }
            } else if (action == "delete" && serverId != null) {
                val lock = deleteLocks.computeIfAbsent(serverId) { Mutex() }
                return lock.withLock {
                    val response = api.deleteTransaction(serverId)
                    if (response.isSuccessful || response.code() == 404) {
                        pendingDeleteManager.removePendingDeletion(serverId)
                        return true
                    }
                    false
                }
            }
            return true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            return false
        }
    }
}
