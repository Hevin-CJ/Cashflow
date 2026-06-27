package com.hevincj.cashflow.data.mapper

import com.hevincj.cashflow.data.local.entity.CreditCardEntity
import com.hevincj.cashflow.domain.models.CreditCard
import kotlinx.collections.immutable.toImmutableList

fun CreditCardEntity.toDomain(): CreditCard {
    return CreditCard(
        id = serverId ?: id.toString(),
        balance = balance,
        cardNumber = cardNumber,
        cardHolder = cardHolder,
        gradientColors = gradientColorsHex.split(",").map { it.toLong(16) }.toImmutableList()
    )
}

fun CreditCard.toEntity(): CreditCardEntity {
    val isServerId = id.isNotEmpty() && !id.all { it.isDigit() }
    return CreditCardEntity(
        id = if (isServerId || id.isEmpty()) 0 else id.toInt(),
        balance = balance,
        cardNumber = cardNumber,
        cardHolder = cardHolder,
        gradientColorsHex = gradientColors.joinToString(",") { it.toString(16) },
        serverId = if (isServerId) id else null,
        isSynced = isServerId
    )
}
