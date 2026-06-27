package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.repository.TransactionRepository
import javax.inject.Inject

class GetTransactionByIdUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(id: String): Transaction? {
        return repository.getTransactionById(id)
    }
}
