package com.hevincj.cashflow

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.hevincj.cashflow.data.worker.RecurringExpenseScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CashFlowApp : Application(), Configuration.Provider {

    @Inject
    lateinit var recurringExpenseScheduler: RecurringExpenseScheduler

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        recurringExpenseScheduler.schedulePeriodicRecurringCheck()
        recurringExpenseScheduler.triggerOneTimeCheck()
    }
}
