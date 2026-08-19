package com.hevincj.cashflow.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class SyncBudgetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncBudgetWorkerEntryPoint {
        fun budgetSyncManager(): BudgetSyncManager
    }

    override suspend fun doWork(): Result {
        val action = inputData.getString("action") ?: return Result.failure()
        val localId = inputData.getInt("local_id", -1)
        val serverId = inputData.getString("server_id")

        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                SyncBudgetWorkerEntryPoint::class.java
            )
            val syncManager = entryPoint.budgetSyncManager()
            val error = syncManager.syncSpecificBudget(action, localId, serverId)
            if (error == null) {
                Result.success()
            } else {
                com.hevincj.cashflow.utils.CrashLogger.w(
                    "SyncBudgetWorker",
                    "Budget sync attempt $runAttemptCount failed for action=$action, localId=$localId: $error"
                )
                if (runAttemptCount < 3) Result.retry() else {
                    com.hevincj.cashflow.utils.CrashLogger.e(
                        "SyncBudgetWorker",
                        "Budget sync permanently failed for action=$action, localId=$localId: $error"
                    )
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.e(
                "SyncBudgetWorker",
                "Unhandled exception in SyncBudgetWorker at attempt $runAttemptCount",
                e
            )
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
