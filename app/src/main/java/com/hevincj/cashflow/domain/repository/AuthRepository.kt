package com.hevincj.cashflow.domain.repository

interface AuthRepository {
    suspend fun initiateLogin(username: String, password: String): Result<Unit>
    suspend fun verifyLogin(username: String, otp: String): Result<Unit>
    suspend fun initiateRegister(username: String, password: String, firstName: String?, lastName: String?, phoneNumber: String?): Result<Unit>
    suspend fun verifyRegister(username: String, otp: String): Result<Unit>
    suspend fun logout()
    fun isLoggedIn(): Boolean
}
