package com.hevincj.cashflow.domain.usecase

import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.repository.TransactionRepository
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction) {
        repository.deleteTransaction(transaction)
    }
}
