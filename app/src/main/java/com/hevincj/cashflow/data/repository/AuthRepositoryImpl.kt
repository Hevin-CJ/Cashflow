package com.hevincj.cashflow.data.repository

import com.hevincj.cashflow.data.local.TokenManager
import com.hevincj.cashflow.data.remote.api.AuthApi
import com.hevincj.cashflow.data.remote.models.LoginRequestDto
import com.hevincj.cashflow.data.remote.models.RegisterRequestDto
import com.hevincj.cashflow.domain.repository.AuthRepository
import com.hevincj.cashflow.data.local.CashFlowDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val tokenManager: TokenManager,
    private val database: CashFlowDatabase
) : AuthRepository {
    override suspend fun initiateLogin(username: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.loginInitiate(LoginRequestDto(username, password))
            if (response.isSuccessful) {
                com.hevincj.cashflow.utils.CrashLogger.i("AuthRepository", "Login initiated for user: $username")
                Result.success(Unit)
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                com.hevincj.cashflow.utils.CrashLogger.w("AuthRepository", "Login initiation failed for $username: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("AuthRepository", "Exception initiating login for $username", e)
            Result.failure(e)
        }
    }

    override suspend fun verifyLogin(username: String, otp: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.loginVerify(com.hevincj.cashflow.data.remote.models.VerifyOtpRequestDto(username, otp))
            if (response.isSuccessful) {
                response.body()?.token?.let {
                    tokenManager.saveToken(it)
                    com.hevincj.cashflow.utils.CrashLogger.setUserId(username)
                    com.hevincj.cashflow.utils.CrashLogger.setCustomKey("is_authenticated", true)
                    com.hevincj.cashflow.utils.CrashLogger.i("AuthRepository", "User $username verified login successfully")
                    Result.success(Unit)
                } ?: Result.failure(Exception("No token in response"))
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                com.hevincj.cashflow.utils.CrashLogger.w("AuthRepository", "Login verification failed for $username: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("AuthRepository", "Exception verifying login for $username", e)
            Result.failure(e)
        }
    }

    override suspend fun initiateRegister(username: String, password: String, firstName: String?, lastName: String?, phoneNumber: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.registerInitiate(RegisterRequestDto(username, password, firstName, lastName, phoneNumber))
            if (response.isSuccessful) {
                com.hevincj.cashflow.utils.CrashLogger.i("AuthRepository", "Register initiated for user: $username")
                Result.success(Unit)
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                com.hevincj.cashflow.utils.CrashLogger.w("AuthRepository", "Register initiation failed for $username: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("AuthRepository", "Exception initiating register for $username", e)
            Result.failure(e)
        }
    }

    override suspend fun verifyRegister(username: String, otp: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.registerVerify(com.hevincj.cashflow.data.remote.models.VerifyOtpRequestDto(username, otp))
            if (response.isSuccessful) {
                response.body()?.token?.let {
                    tokenManager.saveToken(it)
                    com.hevincj.cashflow.utils.CrashLogger.setUserId(username)
                    com.hevincj.cashflow.utils.CrashLogger.setCustomKey("is_authenticated", true)
                    com.hevincj.cashflow.utils.CrashLogger.i("AuthRepository", "User $username registered successfully")
                    Result.success(Unit)
                } ?: Result.failure(Exception("No token in response"))
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                com.hevincj.cashflow.utils.CrashLogger.w("AuthRepository", "Register verification failed for $username: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("AuthRepository", "Exception verifying register for $username", e)
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        withContext(Dispatchers.IO) {
            tokenManager.clearToken()
            database.userProfileDao.clearProfile()
            database.clearAllTables()
            com.hevincj.cashflow.utils.CrashLogger.setUserId("")
            com.hevincj.cashflow.utils.CrashLogger.setCustomKey("is_authenticated", false)
            com.hevincj.cashflow.utils.CrashLogger.i("AuthRepository", "User logged out")
        }
    }

    override fun isLoggedIn(): Boolean {
        return tokenManager.getToken() != null
    }
}