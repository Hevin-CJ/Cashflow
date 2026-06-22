package com.hevincj.cashflow.data.mapper

import com.hevincj.cashflow.data.local.entity.BudgetEntity
import com.hevincj.cashflow.domain.models.Budget
import com.hevincj.cashflow.domain.models.TransactionCategory

fun BudgetEntity.toDomain(spent: Double = 0.0): Budget {
    return Budget(
        category = TransactionCategory.fromString(category),
        monthlyLimit = monthlyLimit,
        spent = spent,
        month = month,
        year = year
    )
}

fun Budget.toEntity(): BudgetEntity {
    return BudgetEntity(
        category = category.name,
        monthlyLimit = monthlyLimit,
        month = month,
        year = year
    )
}
