package com.hevincj.cashflow.data.worker

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionSyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun scheduleUpsertSync(localId: Int) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncTransactionWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        "action" to "upsert",
                        "local_id" to localId
                    )
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "sync_upsert_$localId",
                ExistingWorkPolicy.KEEP,
                syncRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun scheduleDeleteSync(serverId: String) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncTransactionWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        "action" to "delete",
                        "server_id" to serverId
                    )
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "sync_delete_$serverId",
                ExistingWorkPolicy.KEEP,
                syncRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
