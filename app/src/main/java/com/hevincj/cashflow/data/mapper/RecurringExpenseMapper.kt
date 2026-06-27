package com.hevincj.cashflow.data.mapper

import com.hevincj.cashflow.data.local.entity.RecurringExpenseEntity
import com.hevincj.cashflow.domain.models.RecurringExpense
import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.domain.models.RecurringFrequency
import com.hevincj.cashflow.data.remote.models.RecurringExpenseDto
import com.hevincj.cashflow.data.remote.models.RecurringExpenseRequestDto

// Replace the RecurringExpenseEntity.toDomain() function with this optimized version:
fun RecurringExpenseEntity.toDomain(): RecurringExpense {
    val domainId = serverId ?: id.toString()
    return RecurringExpense(
        id = domainId,
        localId = id,
        serverId = serverId,
        isSynced = isSynced,
        frequency = frequency,
        startDate = startDate,
        lastProcessedDate = lastProcessedDate,
        nextDueDate = nextDueDate,
        transaction = Transaction(
            id = "",
            title = title,
            timestamp = startDate,
            amount = amount,
            icon = category.icon,
            iconBgColor = category.iconBgColor,
            type = type,
            category = category,
            description = description,
            isSynced = isSynced,
            recurringExpenseId = domainId
        )
    )
}

fun RecurringExpense.toEntity(): RecurringExpenseEntity {
    val isLocalId = id.all { it.isDigit() }
    val localId = if (isLocalId && id.isNotEmpty()) id.toInt() else 0
    val sId = if (!isLocalId) id else serverId
    return RecurringExpenseEntity(
        id = localId,
        serverId = sId,
        isSynced = isSynced,
        title = transaction.title,
        amount = transaction.amount,
        category = transaction.category,
        type = transaction.type,
        frequency = frequency,
        startDate = startDate,
        lastProcessedDate = lastProcessedDate,
        nextDueDate = nextDueDate,
        description = transaction.description
    )
}

fun RecurringExpenseDto.toDomain(): RecurringExpense {
    val resolvedCategory = TransactionCategory.fromString(category)
    val resolvedType = try { TransactionType.valueOf(type.uppercase(java.util.Locale.ROOT)) } catch (e: Exception) { TransactionType.EXPENSE }
    return RecurringExpense(
        id = id,
        serverId = id,
        isSynced = true,
        frequency = RecurringFrequency.fromString(frequency),
        startDate = startDate,
        lastProcessedDate = lastProcessedDate,
        nextDueDate = nextDueDate,
        transaction = Transaction(
            id = "",
            title = title,
            timestamp = startDate,
            amount = amount,
            icon = resolvedCategory.icon,
            iconBgColor = resolvedCategory.iconBgColor,
            type = resolvedType,
            category = resolvedCategory,
            description = description,
            isSynced = true,
            recurringExpenseId = id
        )
    )
}

fun RecurringExpense.toRequestDto(): RecurringExpenseRequestDto {
    return RecurringExpenseRequestDto(
        title = transaction.title,
        amount = transaction.amount,
        category = transaction.category.name,
        type = transaction.type.name,
        frequency = frequency.name,
        startDate = startDate,
        lastProcessedDate = lastProcessedDate,
        nextDueDate = nextDueDate,
        description = transaction.description
    )
}
