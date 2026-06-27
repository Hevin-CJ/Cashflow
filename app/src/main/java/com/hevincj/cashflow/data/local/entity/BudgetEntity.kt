package com.hevincj.cashflow.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hevincj.cashflow.domain.models.TransactionCategory

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["category", "month", "year"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serverId: String? = null,
    val isSynced: Boolean = false,
    val category: TransactionCategory,
    val monthlyLimit: Double,
    val month: Int,
    val year: Int
)
