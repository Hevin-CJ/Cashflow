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
    val resolvedType = try { TransactionType.valueOf(type.uppercase(java.util.Locale.ROOT)) } catch (e: Exception) { TransactionType.EXPENSE }
    val resolvedAmount = if (resolvedType == TransactionType.EXPENSE) -kotlin.math.abs(amount) else kotlin.math.abs(amount)
    val resolvedCategory = TransactionCategory.fromString(category)
    return Transaction(
        id = serverId ?: id.toString(),
        title = title,
        timestamp = timestamp,
        amount = resolvedAmount,
        icon = resolvedCategory.icon,
        iconBgColor = Color(iconBgColor),
        type = resolvedType,
        category = resolvedCategory,
        description = description,
        isSynced = isSynced,
        barcode = barcode,
        productName = productName,
        formattedDate = DateTimeUtils.formatTimestamp(timestamp)
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
        type = type.name,
        category = category.name,
        description = description,
        isSynced = isSynced,
        barcode = barcode,
        productName = productName
    )
}

fun TransactionDto.toDomain(): Transaction {
    val resolvedCategory = TransactionCategory.fromString(category)
    val displayTitle = if (!description.isNullOrBlank()) description else resolvedCategory.displayName
    val resolvedType = try { TransactionType.valueOf(type.uppercase(java.util.Locale.ROOT)) } catch (e: Exception) { TransactionType.EXPENSE }
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
        description = description,
        isSynced = true,
        formattedDate = DateTimeUtils.formatTimestamp(timestamp)
    )
}
