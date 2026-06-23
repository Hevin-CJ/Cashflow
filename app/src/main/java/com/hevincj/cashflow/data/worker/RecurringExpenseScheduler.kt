package com.hevincj.cashflow.data.worker

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringExpenseScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun schedulePeriodicRecurringCheck() {
        try {
            val constraints = Constraints.Builder()
                .build()

            val recurringCheckRequest = PeriodicWorkRequestBuilder<RecurringExpenseWorker>(
                24, TimeUnit.HOURS,
                1, TimeUnit.HOURS
            )
            .setConstraints(constraints)
            .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "recurring_expense_check",
                ExistingPeriodicWorkPolicy.KEEP,
                recurringCheckRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun triggerOneTimeCheck() {
        try {
            val request = OneTimeWorkRequestBuilder<RecurringExpenseWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
