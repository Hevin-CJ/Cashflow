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
                Result.success(Unit)
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyLogin(username: String, otp: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.loginVerify(com.hevincj.cashflow.data.remote.models.VerifyOtpRequestDto(username, otp))
            if (response.isSuccessful) {
                response.body()?.token?.let {
                    tokenManager.saveToken(it)
                    Result.success(Unit)
                } ?: Result.failure(Exception("No token in response"))
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun initiateRegister(username: String, password: String, firstName: String?, lastName: String?, phoneNumber: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.registerInitiate(RegisterRequestDto(username, password, firstName, lastName, phoneNumber))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyRegister(username: String, otp: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.registerVerify(com.hevincj.cashflow.data.remote.models.VerifyOtpRequestDto(username, otp))
            if (response.isSuccessful) {
                response.body()?.token?.let {
                    tokenManager.saveToken(it)
                    Result.success(Unit)
                } ?: Result.failure(Exception("No token in response"))
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        withContext(Dispatchers.IO) {
            tokenManager.clearToken()
            database.userProfileDao.clearProfile()
            database.clearAllTables()
        }
    }

    override fun isLoggedIn(): Boolean {
        return tokenManager.getToken() != null
    }
}