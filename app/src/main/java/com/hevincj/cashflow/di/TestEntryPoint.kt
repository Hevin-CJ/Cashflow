package com.hevincj.cashflow.di

import com.hevincj.cashflow.domain.repository.RecurringExpenseRepository
import com.hevincj.cashflow.domain.repository.TransactionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TestEntryPoint {
    fun recurringRepository(): RecurringExpenseRepository
    fun transactionRepository(): TransactionRepository
}
