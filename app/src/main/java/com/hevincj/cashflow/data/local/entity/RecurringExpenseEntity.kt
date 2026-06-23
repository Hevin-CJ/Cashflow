package com.hevincj.cashflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_expenses")
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serverId: String? = null,
    val isSynced: Boolean = false,
    val title: String,
    val amount: Double,
    val category: String,
    val type: String = "EXPENSE",
    val frequency: String = "MONTHLY",
    val startDate: Long,
    val lastProcessedDate: Long? = null,
    val nextDueDate: Long,
    val description: String? = null
)
