package com.hevincj.cashflow.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.domain.models.TransactionCategory

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["timestamp"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serverId: String? = null,
    val title: String,
    val timestamp: Long,
    val amount: Double,
    val iconName: String,
    val iconBgColor: Int,
    val type: TransactionType = TransactionType.EXPENSE,
    val category: TransactionCategory = TransactionCategory.OTHERS,
    val description: String? = null,
    val isSynced: Boolean = false,
    val barcode: String? = null,
    val productName: String? = null,
    val lastModifiedLocal: Long = 0L,
    val recurringExpenseId: String? = null
)
