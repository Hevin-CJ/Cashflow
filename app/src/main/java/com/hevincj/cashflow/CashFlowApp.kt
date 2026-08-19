package com.hevincj.cashflow

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.hevincj.cashflow.data.worker.RecurringExpenseSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CashFlowApp : Application(), Configuration.Provider {

    @Inject
    lateinit var recurringExpenseSyncScheduler: RecurringExpenseSyncScheduler

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        recurringExpenseSyncScheduler.schedulePeriodicRecurringProcessing()
    }
}
