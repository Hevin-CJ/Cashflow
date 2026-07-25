package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend fun initiate(username: String, password: String): Result<Unit> {
        return repository.initiateLogin(username, password)
    }

    suspend fun verify(username: String, otp: String): Result<Unit> {
        return repository.verifyLogin(username, otp)
    }
}
