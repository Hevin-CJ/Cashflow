package com.hevincj.cashflow.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class SyncTransactionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncTransactionWorkerEntryPoint {
        fun transactionSyncManager(): TransactionSyncManager
    }

    override suspend fun doWork(): Result {
        return try {
            val appContext = applicationContext
            val entryPoint = EntryPointAccessors.fromApplication(
                appContext,
                SyncTransactionWorkerEntryPoint::class.java
            )
            val syncManager = entryPoint.transactionSyncManager()
            
            val action = inputData.getString("action") ?: "sync_all"
            val localId = inputData.getInt("local_id", -1)
            val serverId = inputData.getString("server_id")

            val error = if (action == "sync_all") {
                // Fallback to sync all (though deprecated, keep as a safe no-op or fallback)
                null
            } else {
                syncManager.syncSpecificTransaction(action, localId, serverId)
            }
            
            if (error == null) {
                Result.success()
            } else {
                com.hevincj.cashflow.utils.CrashLogger.w(
                    "SyncTransactionWorker",
                    "Transaction sync attempt $runAttemptCount failed for action=$action, localId=$localId: $error"
                )
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    com.hevincj.cashflow.utils.CrashLogger.e(
                        "SyncTransactionWorker",
                        "Transaction sync permanently failed for action=$action, localId=$localId: $error"
                    )
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.e(
                "SyncTransactionWorker",
                "Unhandled exception in SyncTransactionWorker at attempt $runAttemptCount",
                e
            )
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
