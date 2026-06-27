package com.hevincj.cashflow.domain.models

enum class RecurringFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    companion object {
        fun fromString(value: String): RecurringFrequency {
            return try {
                valueOf(value.uppercase(java.util.Locale.ROOT))
            } catch (e: Exception) {
                MONTHLY
            }
        }
    }
}
