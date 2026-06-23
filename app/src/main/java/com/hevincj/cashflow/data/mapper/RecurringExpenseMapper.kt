package com.hevincj.cashflow.data.mapper

import com.hevincj.cashflow.data.local.entity.RecurringExpenseEntity
import com.hevincj.cashflow.domain.models.RecurringExpense
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.data.remote.models.RecurringExpenseDto
import com.hevincj.cashflow.data.remote.models.RecurringExpenseRequestDto

fun RecurringExpenseEntity.toDomain(): RecurringExpense {
    val resolvedType = try { TransactionType.valueOf(type.uppercase(java.util.Locale.ROOT)) } catch (e: Exception) { TransactionType.EXPENSE }
    val resolvedCategory = TransactionCategory.fromString(category)
    return RecurringExpense(
        id = serverId ?: id.toString(),
        serverId = serverId,
        isSynced = isSynced,
        title = title,
        amount = amount,
        category = resolvedCategory,
        type = resolvedType,
        frequency = frequency,
        startDate = startDate,
        lastProcessedDate = lastProcessedDate,
        nextDueDate = nextDueDate,
        description = description
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
        title = title,
        amount = amount,
        category = category.name,
        type = type.name,
        frequency = frequency,
        startDate = startDate,
        lastProcessedDate = lastProcessedDate,
        nextDueDate = nextDueDate,
        description = description
    )
}

fun RecurringExpenseDto.toDomain(): RecurringExpense {
    val resolvedCategory = TransactionCategory.fromString(category)
    val resolvedType = try { TransactionType.valueOf(type.uppercase(java.util.Locale.ROOT)) } catch (e: Exception) { TransactionType.EXPENSE }
    return RecurringExpense(
        id = id,
        serverId = id,
        isSynced = true,
        title = title,
        amount = amount,
        category = resolvedCategory,
        type = resolvedType,
        frequency = frequency,
        startDate = startDate,
        lastProcessedDate = lastProcessedDate,
        nextDueDate = nextDueDate,
        description = description
    )
}

fun RecurringExpense.toRequestDto(): RecurringExpenseRequestDto {
    return RecurringExpenseRequestDto(
        title = title,
        amount = amount,
        category = category.name,
        type = type.name,
        frequency = frequency,
        startDate = startDate,
        lastProcessedDate = lastProcessedDate,
        nextDueDate = nextDueDate,
        description = description
    )
}
