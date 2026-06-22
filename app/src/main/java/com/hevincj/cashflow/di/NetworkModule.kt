package com.hevincj.cashflow.di

import android.content.Context
import com.hevincj.cashflow.data.local.TokenManager
import com.hevincj.cashflow.data.remote.api.AuthApi
import com.hevincj.cashflow.data.remote.api.AuthInterceptor
import com.hevincj.cashflow.data.remote.api.SignatureInterceptor
import com.hevincj.cashflow.data.remote.api.TransactionApi
import com.hevincj.cashflow.data.remote.api.CardsApi
import com.hevincj.cashflow.data.remote.api.ScanApi
import com.hevincj.cashflow.BuildConfig

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
        return TokenManager(context)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): AuthInterceptor {
        return AuthInterceptor(tokenManager)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // BODY logging buffers the full response and is expensive; disable in production.
            // Change to HttpLoggingInterceptor.Level.BODY only for local debug sessions.
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        
        val certificatePinner = CertificatePinner.Builder()
            .add("cashflow-ktor-backend-703934017156.asia-south2.run.app", "sha256/eJE3WecPmXBRe5q9uM/elt/q85QCTTy1ZhkCAMA7NI4=")
            .add("cashflow-ktor-backend-703934017156.asia-south2.run.app", "sha256/vh78KSg1Ry4NaqGDV10w/cTb9VH3BQUZoCWNa93W/EY=")
            .add("cashflow-ktor-backend-703934017156.asia-south2.run.app", "sha256/mEflZT5enoR1FuXLgYYGqnVEoZvmf9c2bVBpiOjYQ0c=")
            .build()

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(SignatureInterceptor())
            .addInterceptor(loggingInterceptor)
            .certificatePinner(certificatePinner)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://cashflow-ktor-backend-703934017156.asia-south2.run.app/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTransactionApi(retrofit: Retrofit): TransactionApi {
        return retrofit.create(TransactionApi::class.java)
    }

    @Provides
    @Singleton
    fun provideCardsApi(retrofit: Retrofit): CardsApi {
        return retrofit.create(CardsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideScanApi(retrofit: Retrofit): ScanApi {
        return retrofit.create(ScanApi::class.java)
    }
}