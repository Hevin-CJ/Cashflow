package com.hevincj.cashflow.data.worker

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetSyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun scheduleUpsertSync(localId: Int) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncBudgetWorker>()
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
                "sync_budget_upsert_$localId",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("BudgetSyncScheduler", "Failed to schedule budget upsert sync for $localId: ${e.message}", e)
        }
    }

    fun scheduleDeleteSync(category: String, month: Int, year: Int, serverId: String) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncBudgetWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        "action" to "delete",
                        "server_id" to serverId,
                        "category" to category,
                        "month" to month,
                        "year" to year
                    )
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "sync_budget_delete_${category}_${month}_${year}",
                ExistingWorkPolicy.KEEP,
                syncRequest
            )
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("BudgetSyncScheduler", "Failed to schedule budget delete sync for $serverId: ${e.message}", e)
        }
    }
}
