package com.hevincj.cashflow.di

import android.app.Application
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.room.Room
import com.hevincj.cashflow.data.local.CashFlowDatabase
import com.hevincj.cashflow.data.local.dao.TransactionDao
import com.hevincj.cashflow.data.local.PendingDeleteManager
import com.hevincj.cashflow.data.repository.TransactionRepositoryImpl
import com.hevincj.cashflow.domain.repository.TransactionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.hevincj.cashflow.data.local.dao.CreditCardDao
import com.hevincj.cashflow.data.remote.api.AuthApi
import com.hevincj.cashflow.data.remote.api.TransactionApi
import com.hevincj.cashflow.data.remote.api.CardsApi
import com.hevincj.cashflow.data.repository.AuthRepositoryImpl
import com.hevincj.cashflow.data.repository.CreditCardRepositoryImpl
import com.hevincj.cashflow.domain.repository.AuthRepository
import com.hevincj.cashflow.domain.repository.CreditCardRepository
import com.hevincj.cashflow.data.local.TokenManager
import com.hevincj.cashflow.data.repository.ScanRepositoryImpl
import com.hevincj.cashflow.data.worker.TransactionSyncScheduler
import com.hevincj.cashflow.data.worker.TransactionSyncManager
import com.hevincj.cashflow.domain.repository.ScanRepository

import com.hevincj.cashflow.data.local.dao.RecurringExpenseDao
import com.hevincj.cashflow.data.remote.api.RecurringExpenseApi
import com.hevincj.cashflow.domain.repository.RecurringExpenseRepository
import com.hevincj.cashflow.data.repository.RecurringExpenseRepositoryImpl
import com.hevincj.cashflow.data.worker.RecurringExpenseSyncScheduler
import com.hevincj.cashflow.data.worker.RecurringExpenseSyncManager

import com.hevincj.cashflow.data.remote.api.BudgetApi
import com.hevincj.cashflow.data.worker.BudgetSyncScheduler
import com.hevincj.cashflow.data.worker.BudgetSyncManager
import com.hevincj.cashflow.data.remote.api.UserApi
import com.hevincj.cashflow.domain.repository.UserRepository
import com.hevincj.cashflow.data.repository.UserRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        api: AuthApi,
        tokenManager: TokenManager,
        database: CashFlowDatabase
    ): AuthRepository {
        return AuthRepositoryImpl(api, tokenManager, database)
    }

    @Provides
    @Singleton
    fun provideCashFlowDatabase(app: Application): CashFlowDatabase {
        return Room.databaseBuilder(
            app,
            CashFlowDatabase::class.java,
            CashFlowDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration(dropAllTables = true)
        .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .build()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(db: CashFlowDatabase): TransactionDao {
        return db.transactionDao
    }

    @Provides
    @Singleton
    fun provideCreditCardDao(db: CashFlowDatabase): CreditCardDao {
        return db.creditCardDao
    }

    @Provides
    @Singleton
    fun provideTransactionRepository(
        dao: TransactionDao,
        api: TransactionApi,
        syncScheduler: TransactionSyncScheduler,
        pendingDeleteManager: PendingDeleteManager,
        syncManager: TransactionSyncManager,
        @ApplicationContext context: Context
    ): TransactionRepository {
        return TransactionRepositoryImpl(dao, api, syncScheduler, pendingDeleteManager, syncManager, context)
    }

    @Provides
    @Singleton
    fun provideCreditCardRepository(
        dao: CreditCardDao,
        api: CardsApi
    ): CreditCardRepository {
        return CreditCardRepositoryImpl(dao, api)
    }

    @Provides
    @Singleton
    fun provideBudgetDao(db: CashFlowDatabase): com.hevincj.cashflow.data.local.dao.BudgetDao {
        return db.budgetDao
    }

    @Provides
    @Singleton
    fun provideBudgetRepository(
        dao: com.hevincj.cashflow.data.local.dao.BudgetDao,
        api: BudgetApi,
        syncScheduler: BudgetSyncScheduler,
        pendingDeleteManager: PendingDeleteManager,
        syncManager: BudgetSyncManager
    ): com.hevincj.cashflow.domain.repository.BudgetRepository {
        return com.hevincj.cashflow.data.repository.BudgetRepositoryImpl(
            dao = dao,
            api = api,
            syncScheduler = syncScheduler,
            pendingDeleteManager = pendingDeleteManager,
            syncManager = syncManager
        )
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(app: android.app.Application): com.hevincj.cashflow.utils.NetworkMonitor {
        return com.hevincj.cashflow.utils.NetworkMonitor(app)
    }

    @Provides
    @Singleton
    fun provideScanRepository(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
        scanApi: com.hevincj.cashflow.data.remote.api.ScanApi
    ): ScanRepository {
        return ScanRepositoryImpl(context, scanApi)
    }

    @Provides
    @Singleton
    fun provideRecurringExpenseDao(db: CashFlowDatabase): RecurringExpenseDao {
        return db.recurringExpenseDao
    }

    @Provides
    @Singleton
    fun provideRecurringExpenseRepository(
        dao: RecurringExpenseDao,
        api: RecurringExpenseApi,
        syncScheduler: RecurringExpenseSyncScheduler,
        pendingDeleteManager: PendingDeleteManager,
        syncManager: RecurringExpenseSyncManager
    ): RecurringExpenseRepository {
        return RecurringExpenseRepositoryImpl(dao, api, syncScheduler, pendingDeleteManager, syncManager)
    }

    @Provides
    @Singleton
    fun provideUserRepository(api: UserApi): UserRepository {
        return UserRepositoryImpl(api)
    }

    @Provides
    @Singleton
    fun provideExchangeRepository(
        api: com.hevincj.cashflow.data.remote.api.ExchangeApi,
        dao: com.hevincj.cashflow.data.local.dao.ExchangeRateDao
    ): com.hevincj.cashflow.domain.repository.ExchangeRepository {
        return com.hevincj.cashflow.data.repository.ExchangeRepositoryImpl(api, dao)
    }

    @Provides
    @Singleton
    fun provideExchangeRateDao(db: CashFlowDatabase): com.hevincj.cashflow.data.local.dao.ExchangeRateDao {
        return db.exchangeRateDao
    }

    @Provides
    @Singleton
    fun provideUpdateRepository(
        githubApi: com.hevincj.cashflow.data.remote.api.GithubApi
    ): com.hevincj.cashflow.domain.repository.UpdateRepository {
        return com.hevincj.cashflow.data.repository.UpdateRepositoryImpl(githubApi)
    }
}
