package com.hevincj.cashflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.models.RecurringFrequency

@Entity(tableName = "recurring_expenses")
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serverId: String? = null,
    val isSynced: Boolean = false,
    val title: String,
    val amount: Double,
    val category: TransactionCategory,
    val type: TransactionType = TransactionType.EXPENSE,
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val startDate: Long,
    val lastProcessedDate: Long? = null,
    val nextDueDate: Long,
    val description: String? = null
)
