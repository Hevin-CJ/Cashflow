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

            val success = if (action == "sync_all") {
                // Fallback to sync all (though deprecated, keep as a safe no-op or fallback)
                true
            } else {
                syncManager.syncSpecificTransaction(action, localId, serverId)
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
