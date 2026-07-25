package com.hevincj.cashflow.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hevincj.cashflow.domain.usecase.ProcessRecurringExpensesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@HiltWorker
class RecurringExpenseWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val processUseCase: ProcessRecurringExpensesUseCase
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RecurringExpenseWorkerEntryPoint {
        fun processRecurringExpensesUseCase(): ProcessRecurringExpensesUseCase
    }

    override suspend fun doWork(): Result {
        return try {
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