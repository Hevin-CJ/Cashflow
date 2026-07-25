package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend fun initiate(username: String, password: String, firstName: String?, lastName: String?, phoneNumber: String?): Result<Unit> {
        return repository.initiateRegister(username, password, firstName, lastName, phoneNumber)
     }

    suspend fun verify(username: String, otp: String): Result<Unit> {
        return repository.verifyRegister(username, otp)
    }
}
