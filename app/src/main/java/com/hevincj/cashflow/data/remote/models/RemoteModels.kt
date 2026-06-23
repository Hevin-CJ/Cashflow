package com.hevincj.cashflow.data.remote.models


data class LoginRequestDto(
    val username: String,
    val password: String
)

data class RegisterRequestDto(
    val username: String,
    val password: String
)

data class LoginResponseDto(
    val id: String,
    val username: String,
    val token: String
)

data class RegisterResponseDto(
    val id: String,
    val username: String
)

data class TransactionDto(
    val id: String,
    val userId: String,
    val amount: Double,
    val type: String,
    val category: String,
    val description: String?,
    val timestamp: Long
)

data class TransactionRequestDto(
    val amount: Double,
    val type: String,
    val category: String,
    val description: String? = null,
    val barcode: String? = null,
    val productName: String? = null
)

data class CreditCardDto(
    val id: String,
    val userId: String,
    val balance: Double,
    val cardNumber: String,
    val cardHolder: String,
    val gradientColors: List<Long>
)

data class CreditCardRequestDto(
    val balance: Double,
    val cardNumber: String,
    val cardHolder: String,
    val gradientColors: List<Long>
)

data class RecurringExpenseDto(
    val id: String,
    val userId: String,
    val title: String,
    val amount: Double,
    val category: String,
    val type: String,
    val frequency: String,
    val startDate: Long,
    val lastProcessedDate: Long?,
    val nextDueDate: Long,
    val description: String?
)

data class RecurringExpenseRequestDto(
    val title: String,
    val amount: Double,
    val category: String,
    val type: String,
    val frequency: String,
    val startDate: Long,
    val lastProcessedDate: Long?,
    val nextDueDate: Long,
    val description: String?
)

data class BudgetDto(
    val id: String,
    val userId: String,
    val category: String,
    val monthlyLimit: Double,
    val month: Int,
    val year: Int
)

data class BudgetRequestDto(
    val category: String,
    val monthlyLimit: Double,
    val month: Int,
    val year: Int
)
