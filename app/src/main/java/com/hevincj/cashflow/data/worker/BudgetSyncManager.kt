package com.hevincj.cashflow.data.worker

import com.hevincj.cashflow.data.local.dao.BudgetDao
import com.hevincj.cashflow.data.local.PendingDeleteManager
import com.hevincj.cashflow.data.remote.api.BudgetApi
import com.hevincj.cashflow.data.remote.models.BudgetRequestDto
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class BudgetSyncManager @Inject constructor(
    private val dao: BudgetDao,
    private val api: BudgetApi,
    private val pendingDeleteManager: PendingDeleteManager
) {
    private val budgetLocks = ConcurrentHashMap<Int, Mutex>()
    private val deleteLocks = ConcurrentHashMap<String, Mutex>()

    suspend fun syncSpecificBudget(action: String, localId: Int, serverId: String?): String? {
        try {
            if (action == "upsert" && localId != -1) {
                val lock = budgetLocks.computeIfAbsent(localId) { Mutex() }
                return lock.withLock {
                    val entity = dao.getBudgetById(localId) ?: return null
                    if (entity.isSynced) return null

                    val requestDto = BudgetRequestDto(
                        category = entity.category.name,
                        monthlyLimit = entity.monthlyLimit,
                        month = entity.month,
                        year = entity.year
                    )

                    if (entity.serverId != null) {
                        val response = api.updateBudget(entity.serverId, requestDto)
                        if (response.isSuccessful) {
                            dao.insertBudget(entity.copy(isSynced = true))
                            return null
                        } else if (response.code() == 404) {
                            // Server-side ID not found (e.g. server DB cleared). Fallback to creating a new budget.
                            val createResponse = api.createBudget(requestDto)
                            if (createResponse.isSuccessful) {
                                createResponse.body()?.let { remoteDto ->
                                    dao.insertBudget(
                                        entity.copy(
                                            isSynced = true,
                                            serverId = remoteDto.id
                                        )
                                    )
                                }
                                return null
                            } else {
                                return "Failed to sync budget (create fallback after 404): ${createResponse.message()} (code ${createResponse.code()})"
                            }
                        } else {
                            return "Failed to update budget: ${response.message()} (code ${response.code()})"
                        }
                    } else {
                        val response = api.createBudget(requestDto)
                        if (response.isSuccessful) {
                            response.body()?.let { remoteDto ->
                                dao.insertBudget(
                                    entity.copy(
                                        isSynced = true,
                                        serverId = remoteDto.id
                                    )
                                )
                            }
                            return null
                        } else {
                            return "Failed to create budget: ${response.message()} (code ${response.code()})"
                        }
                    }
                }
            } else if (action == "delete" && serverId != null) {
                val lock = deleteLocks.computeIfAbsent(serverId) { Mutex() }
                return lock.withLock {
                    val response = api.deleteBudget(serverId)
                    if (response.isSuccessful || response.code() == 404) {
                        pendingDeleteManager.removePendingBudgetDeletion(serverId)
                        return null
                    }
                    return "Failed to delete budget: ${response.message()} (code ${response.code()})"
                }
            }
            return null
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            return e.message ?: "Unknown sync error"
        }
    }
}
