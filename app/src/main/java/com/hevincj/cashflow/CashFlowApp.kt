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
        setupCrashReporting()
        recurringExpenseSyncScheduler.schedulePeriodicRecurringProcessing()
    }

    private fun setupCrashReporting() {
        com.hevincj.cashflow.utils.CrashLogger.setCustomKey("version_name", BuildConfig.VERSION_NAME)
        com.hevincj.cashflow.utils.CrashLogger.setCustomKey("version_code", BuildConfig.VERSION_CODE)
        com.hevincj.cashflow.utils.CrashLogger.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
        com.hevincj.cashflow.utils.CrashLogger.i("CashFlowApp", "Application initialized")

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            com.hevincj.cashflow.utils.CrashLogger.e("UncaughtException", "Uncaught exception on thread: ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
