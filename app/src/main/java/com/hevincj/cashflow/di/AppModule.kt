package com.hevincj.cashflow.di

import android.app.Application
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
        // WAL allows concurrent reads and writes, preventing DB lock contention
        // that causes visible freeze/stutter when the HomeViewModel Flow collector
        // and the background sync write hit the DB simultaneously on startup.
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
        syncManager: TransactionSyncManager
    ): TransactionRepository {
        return TransactionRepositoryImpl(dao, api, syncScheduler, pendingDeleteManager, syncManager)
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
        dao: com.hevincj.cashflow.data.local.dao.BudgetDao
    ): com.hevincj.cashflow.domain.repository.BudgetRepository {
        return com.hevincj.cashflow.data.repository.BudgetRepositoryImpl(dao)
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
}
