package com.hevincj.cashflow

import android.app.Application
import com.hevincj.cashflow.data.worker.RecurringExpenseScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CashFlowApp : Application() {

    @Inject
    lateinit var recurringExpenseScheduler: RecurringExpenseScheduler

    override fun onCreate() {
        super.onCreate()
        recurringExpenseScheduler.schedulePeriodicRecurringCheck()
    }
}
