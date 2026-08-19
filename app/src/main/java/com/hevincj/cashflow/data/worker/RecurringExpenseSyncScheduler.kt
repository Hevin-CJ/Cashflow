package com.hevincj.cashflow.data.worker

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringExpenseSyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    // Triggered on-demand when a subscription is added or updated by the user
    fun triggerImmediateProcessing() {
        try {
            val immediateRequest = OneTimeWorkRequestBuilder<RecurringExpenseWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "immediate_recurring_processing",
                ExistingWorkPolicy.KEEP,
                immediateRequest
            )
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("RecurringExpenseSyncScheduler", "Failed to schedule immediate processing: ${e.message}", e)
        }
    }

    fun schedulePeriodicRecurringProcessing() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<RecurringExpenseWorker>(
                1, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "periodic_recurring_processing",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("RecurringExpenseSyncScheduler", "Failed to schedule periodic processing: ${e.message}", e)
        }
    }

    fun scheduleUpsertSync(localId: Int) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncRecurringExpenseWorker>()
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
                "sync_recurring_upsert_$localId",
                ExistingWorkPolicy.KEEP,
                syncRequest
            )
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("RecurringExpenseSyncScheduler", "Failed to schedule recurring upsert sync for $localId: ${e.message}", e)
        }
    }

    fun scheduleDeleteSync(serverId: String) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncRecurringExpenseWorker>()
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
                "sync_recurring_delete_$serverId",
                ExistingWorkPolicy.KEEP,
                syncRequest
            )
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("RecurringExpenseSyncScheduler", "Failed to schedule recurring delete sync for $serverId: ${e.message}", e)
        }
    }
}