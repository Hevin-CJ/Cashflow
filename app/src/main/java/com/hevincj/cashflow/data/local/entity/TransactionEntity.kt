package com.hevincj.cashflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serverId: String? = null,
    val title: String,
    val timestamp: Long,
    val amount: Double,
    val iconName: String,
    val iconBgColor: Int,
    val type: String = "EXPENSE",
    val category: String = "Others",
    val description: String? = null,
    val isSynced: Boolean = false,
    val barcode: String? = null,
    val productName: String? = null
)
