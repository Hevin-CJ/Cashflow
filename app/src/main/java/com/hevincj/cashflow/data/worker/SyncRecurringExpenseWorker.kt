package com.hevincj.cashflow.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class SyncRecurringExpenseWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncRecurringExpenseWorkerEntryPoint {
        fun recurringExpenseSyncManager(): RecurringExpenseSyncManager
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                SyncRecurringExpenseWorkerEntryPoint::class.java
            )
            val syncManager = entryPoint.recurringExpenseSyncManager()

            val action = inputData.getString("action") ?: "sync_all"
            val localId = inputData.getInt("local_id", -1)
            val serverId = inputData.getString("server_id")

            val success = if (action == "sync_all") {
                true
            } else {
                syncManager.syncSpecificRecurringExpense(action, localId, serverId)
            }

            if (success) {
                Result.success()
            } else {
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
