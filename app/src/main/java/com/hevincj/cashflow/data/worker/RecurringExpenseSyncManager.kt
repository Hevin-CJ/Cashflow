package com.hevincj.cashflow.data.worker

import com.hevincj.cashflow.data.local.dao.RecurringExpenseDao
import com.hevincj.cashflow.data.local.dao.TransactionDao
import com.hevincj.cashflow.data.local.PendingDeleteManager
import com.hevincj.cashflow.data.remote.api.RecurringExpenseApi
import com.hevincj.cashflow.data.remote.models.RecurringExpenseRequestDto
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class RecurringExpenseSyncManager @Inject constructor(
    private val dao: RecurringExpenseDao,
    private val transactionDao: TransactionDao,
    private val api: RecurringExpenseApi,
    private val pendingDeleteManager: PendingDeleteManager
) {
    private val recurringLocks = ConcurrentHashMap<Int, Mutex>()
    private val deleteLocks = ConcurrentHashMap<String, Mutex>()

    suspend fun syncSpecificRecurringExpense(action: String, localId: Int, serverId: String?): Boolean {
        try {
            if (action == "upsert" && localId != -1) {
                val lock = recurringLocks.computeIfAbsent(localId) { Mutex() }
                return lock.withLock {
                    val entity = dao.getRecurringExpenseById(localId) ?: return true
                    if (entity.isSynced) return true

                    val requestDto = RecurringExpenseRequestDto(
                        title = entity.title,
                        amount = entity.amount,
                        category = entity.category.name,
                        type = entity.type.name,
                        frequency = entity.frequency.name,
                        startDate = entity.startDate,
                        lastProcessedDate = entity.lastProcessedDate,
                        nextDueDate = entity.nextDueDate,
                        description = entity.description
                    )

                    if (entity.serverId != null) {
                        val response = api.updateRecurringExpense(entity.serverId, requestDto)
                        if (response.isSuccessful) {
                            dao.insertRecurringExpense(entity.copy(isSynced = true))
                            return true
                        }
                    } else {
                        val response = api.createRecurringExpense(requestDto)
                        if (response.isSuccessful) {
                            response.body()?.let { remoteDto ->
                                dao.insertRecurringExpense(
                                    entity.copy(
                                        isSynced = true,
                                        serverId = remoteDto.id
                                    )
                                )
                                // Cascade the server ID to all pre-existing local transactions
                                transactionDao.updateRecurringExpenseId(localId.toString(), remoteDto.id)
                            }
                            return true
                        }
                    }
                    false
                }
            } else if (action == "delete" && serverId != null) {
                val lock = deleteLocks.computeIfAbsent(serverId) { Mutex() }
                return lock.withLock {
                    val response = api.deleteRecurringExpense(serverId)
                    if (response.isSuccessful || response.code() == 404) {
                        pendingDeleteManager.removePendingRecurringDeletion(serverId)
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
