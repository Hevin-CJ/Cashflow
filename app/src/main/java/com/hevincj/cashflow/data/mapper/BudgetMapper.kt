package com.hevincj.cashflow.data.mapper

import com.hevincj.cashflow.data.local.entity.BudgetEntity
import com.hevincj.cashflow.domain.models.Budget
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.data.remote.models.BudgetDto

fun BudgetEntity.toDomain(spent: Double = 0.0): Budget {
    return Budget(
        id = id,
        serverId = serverId,
        isSynced = isSynced,
        category = category,
        monthlyLimit = monthlyLimit,
        spent = spent,
        month = month,
        year = year
    )
}

fun Budget.toEntity(): BudgetEntity {
    return BudgetEntity(
        id = id,
        serverId = serverId,
        isSynced = isSynced,
        category = category,
        monthlyLimit = monthlyLimit,
        month = month,
        year = year
    )
}

fun BudgetDto.toDomain(spent: Double = 0.0): Budget {
    return Budget(
        serverId = id,
        isSynced = true,
        category = TransactionCategory.fromString(category),
        monthlyLimit = monthlyLimit,
        spent = spent,
        month = month,
        year = year
    )
}
