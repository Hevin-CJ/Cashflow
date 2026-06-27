package com.hevincj.cashflow.data.local

import androidx.room.TypeConverter
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.domain.models.RecurringFrequency

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return try {
            TransactionType.valueOf(value.uppercase(java.util.Locale.ROOT))
        } catch (e: Exception) {
            TransactionType.EXPENSE
        }
    }

    @TypeConverter
    fun fromTransactionCategory(value: TransactionCategory): String = value.name

    @TypeConverter
    fun toTransactionCategory(value: String): TransactionCategory = TransactionCategory.fromString(value)

    @TypeConverter
    fun fromRecurringFrequency(value: RecurringFrequency): String = value.name

    @TypeConverter
    fun toRecurringFrequency(value: String): RecurringFrequency = RecurringFrequency.fromString(value)
}
