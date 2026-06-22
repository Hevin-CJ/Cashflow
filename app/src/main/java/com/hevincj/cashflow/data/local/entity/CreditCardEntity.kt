package com.hevincj.cashflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val balance: Double,
    val cardNumber: String,
    val cardHolder: String,
    val gradientColorsHex: String, // Comma-separated hex values
    val serverId: String? = null,
    val isSynced: Boolean = false
)
