package com.hevincj.cashflow.data.mapper

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.hevincj.cashflow.data.local.entity.TransactionEntity
import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.data.remote.models.TransactionDto
import com.hevincj.cashflow.utils.DateTimeUtils

fun TransactionEntity.toDomain(): Transaction {
    val resolvedAmount = if (type == TransactionType.EXPENSE) -kotlin.math.abs(amount) else kotlin.math.abs(amount)
    return Transaction(
        id = serverId ?: id.toString(),
        title = title,
        timestamp = timestamp,
        amount = resolvedAmount,
        icon = category.icon,
        iconBgColor = Color(iconBgColor),
        type = type,
        category = category,
        description = description,
        isSynced = isSynced,
        barcode = barcode,
        productName = productName,
        formattedDate = DateTimeUtils.formatTimestamp(timestamp),
        lastModifiedLocal = lastModifiedLocal,
        recurringExpenseId = recurringExpenseId
    )
}

fun Transaction.toEntity(): TransactionEntity {
    val isLocalId = id.all { it.isDigit() }
    val localId = if (isLocalId && id.isNotEmpty()) id.toInt() else 0
    val sId = if (!isLocalId) id else null
    val resolvedAmount = if (type == TransactionType.EXPENSE) -kotlin.math.abs(amount) else kotlin.math.abs(amount)
    return TransactionEntity(
        id = localId,
        serverId = sId,
        title = title,
        timestamp = timestamp,
        amount = resolvedAmount,
        iconName = category.iconName,
        iconBgColor = iconBgColor.toArgb(),
        type = type,
        category = category,
        description = description,
        isSynced = isSynced,
        barcode = barcode,
        productName = productName,
        lastModifiedLocal = lastModifiedLocal,
        recurringExpenseId = recurringExpenseId
    )
}

fun TransactionDto.toDomain(): Transaction {
    val resolvedCategory = TransactionCategory.fromString(category)
    val resolvedType = try {
        TransactionType.valueOf(type.uppercase(java.util.Locale.ROOT))
    } catch (e: Exception) {
        TransactionType.EXPENSE
    }

    // Check if the remote description matches an out-of-date or fallback category name string
    val isDescriptionCategoryFallback = !description.isNullOrBlank() && TransactionCategory.values().any {
        it.displayName.equals(description, ignoreCase = true) || it.name.equals(description, ignoreCase = true)
    }

    // If description is a category fallback, dynamically shift the title to match the fresh category
    val displayTitle = if (!description.isNullOrBlank() && !isDescriptionCategoryFallback) {
        description
    } else {
        resolvedCategory.displayName
    }

    val resolvedAmount = if (resolvedType == TransactionType.EXPENSE) -kotlin.math.abs(amount) else kotlin.math.abs(amount)

    return Transaction(
        id = id,
        title = displayTitle,
        timestamp = timestamp,
        amount = resolvedAmount,
        icon = resolvedCategory.icon,
        iconBgColor = resolvedCategory.iconBgColor,
        type = resolvedType,
        category = resolvedCategory,
        description = if (isDescriptionCategoryFallback) null else description,
        isSynced = true,
        formattedDate = DateTimeUtils.formatTimestamp(timestamp),
        recurringExpenseId = recurringExpenseId
    )
}