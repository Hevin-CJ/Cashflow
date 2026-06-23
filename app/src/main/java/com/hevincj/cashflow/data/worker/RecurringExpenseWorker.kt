package com.hevincj.cashflow.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hevincj.cashflow.domain.usecase.ProcessRecurringExpensesUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class RecurringExpenseWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RecurringExpenseWorkerEntryPoint {
        fun processRecurringExpensesUseCase(): ProcessRecurringExpensesUseCase
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                RecurringExpenseWorkerEntryPoint::class.java
            )
            val processUseCase = entryPoint.processRecurringExpensesUseCase()
            processUseCase()
            Result.success()
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
