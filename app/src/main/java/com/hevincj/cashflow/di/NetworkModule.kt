package com.hevincj.cashflow.di

import android.content.Context
import android.os.SystemClock
import com.hevincj.cashflow.data.local.TokenManager
import com.hevincj.cashflow.data.remote.api.AuthApi
import com.hevincj.cashflow.data.remote.api.UserApi
import com.hevincj.cashflow.data.remote.api.AuthInterceptor
import com.hevincj.cashflow.data.remote.api.SignatureInterceptor
import com.hevincj.cashflow.data.remote.api.TransactionApi
import com.hevincj.cashflow.data.remote.api.CardsApi
import com.hevincj.cashflow.data.remote.api.ScanApi
import com.hevincj.cashflow.data.remote.api.BudgetApi
import com.hevincj.cashflow.data.remote.api.RecurringExpenseApi
import com.hevincj.cashflow.BuildConfig

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.CertificatePinner
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
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
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        // FIX 2: Local anonymous Retry Interceptor with progressive delays to absorb temporary link drops
        val retryInterceptor = Interceptor { chain ->
            val request = chain.request()
            var attempt = 0
            var delay = 1000L
            var lastException: java.io.IOException? = null

            while (attempt < 3) {
                var response: okhttp3.Response? = null
                try {
                    response = chain.proceed(request)
                    if (response.isSuccessful || response.code < 500) {
                        return@Interceptor response
                    } else {
                        response.close()
                    }
                } catch (e: java.io.IOException) {
                    com.hevincj.cashflow.utils.CrashLogger.w("NetworkModule", "Retry attempt $attempt failed for ${request.url.encodedPath}: ${e.message}", e)
                    lastException = e
                    response?.close()
                }
                attempt++
                if (attempt >= 3) break
                SystemClock.sleep(delay)
                delay *= 2
            }
            val finalEx = lastException ?: java.io.IOException("Network processing failed after 3 attempts for ${request.url.encodedPath}")
            com.hevincj.cashflow.utils.CrashLogger.e("NetworkModule", "Network retry exhausted for ${request.url.encodedPath}", finalEx)
            throw finalEx
        }

        val certificatePinner = CertificatePinner.Builder()
            // Google Trust Services (GTS) Roots
            .add("cashflow-ktor-backend-703934017156.asia-south2.run.app", "sha256/hxqRlPTu1bMS/0DITB1SSu0vd4u/8l8TjPgfaAp63Gc=") // GTS Root R1
            .add("cashflow-ktor-backend-703934017156.asia-south2.run.app", "sha256/Vfd95BwDeSQo+NUYxVEEIlvkOlWY2SalKK1lPhzOx78=") // GTS Root R2
            .add("cashflow-ktor-backend-703934017156.asia-south2.run.app", "sha256/QXnt2YHvdHR3tJYmQIr0Paosp6t/nggsEGD4QJZ3Q0g=") // GTS Root R3
            .add("cashflow-ktor-backend-703934017156.asia-south2.run.app", "sha256/mEflZT5enoR1FuXLgYYGqnVEoZvmf9c2bVBpiOjYQ0c=") // GTS Root R4 (Active)
            // GlobalSign Roots (used for Google cross-signing/fallback)
            .add("cashflow-ktor-backend-703934017156.asia-south2.run.app", "sha256/CLOmM1/OXvSPjw5UOYbAf9GKOxImEp9hhku9W90fHMk=") // GlobalSign ECC Root R4
            .add("cashflow-ktor-backend-703934017156.asia-south2.run.app", "sha256/cGuxAXyFXFkWm61cF4HPWX8S0srS9j0aSqN0k4AP+4A=") // GlobalSign Root R3
            .add("cashflow-ktor-backend-703934017156.asia-south2.run.app", "sha256/fg6tdrtoGdwvVFEahDVPboswe53YIFjqbABPAdndpd8=") // GlobalSign ECC Root R5
            .add("cashflow-ktor-backend-703934017156.asia-south2.run.app", "sha256/aCdH+LpiG4fN07wpXtXKvOciocDANj0daLOJKNJ4fx4=") // GlobalSign Root R6
            .build()

        return OkHttpClient.Builder()
            // FIX 3: Increase connection allocation boundaries to survive Cloud Run cold container boots
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .certificatePinner(certificatePinner)
            .addInterceptor(authInterceptor)
            .addInterceptor(SignatureInterceptor())
            .addInterceptor(retryInterceptor)
            .addInterceptor(loggingInterceptor)
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
    fun provideUserApi(retrofit: Retrofit): UserApi {
        return retrofit.create(UserApi::class.java)
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

    @Provides
    @Singleton
    fun provideRecurringExpenseApi(retrofit: Retrofit): RecurringExpenseApi {
        return retrofit.create(RecurringExpenseApi::class.java)
    }

    @Provides
    @Singleton
    fun provideBudgetApi(retrofit: Retrofit): BudgetApi {
        return retrofit.create(BudgetApi::class.java)
    }

    @Provides
    @Singleton
    fun provideExchangeApi(): com.hevincj.cashflow.data.remote.api.ExchangeApi {
        return retrofit2.Retrofit.Builder()
            .baseUrl("https://api.frankfurter.app/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(com.hevincj.cashflow.data.remote.api.ExchangeApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGithubApi(): com.hevincj.cashflow.data.remote.api.GithubApi {
        return retrofit2.Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(com.hevincj.cashflow.data.remote.api.GithubApi::class.java)
    }
}