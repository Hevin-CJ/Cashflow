package com.hevincj.cashflow.domain.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class TransactionCategory(
    val displayName: String,
    val iconName: String,
    val supportedTypes: List<TransactionType> = listOf(TransactionType.INCOME, TransactionType.EXPENSE)
) {
    SALARY("Salary", "AccountBalance", listOf(TransactionType.INCOME)),
    FOOD("Food", "Restaurant", listOf(TransactionType.EXPENSE)),
    GROCERIES("Groceries", "ShoppingCart", listOf(TransactionType.EXPENSE)),
    SHOPPING("Shopping", "ShoppingBag", listOf(TransactionType.EXPENSE)),
    TRANSPORT("Transport", "DirectionsCar", listOf(TransactionType.EXPENSE)),
    HOUSING("Housing", "Home", listOf(TransactionType.EXPENSE)),
    UTILITIES("Utilities", "Lightbulb", listOf(TransactionType.EXPENSE)),
    ENTERTAINMENT("Entertainment", "Movie", listOf(TransactionType.EXPENSE)),
    HEALTH("Health", "MedicalServices", listOf(TransactionType.EXPENSE)),
    EDUCATION("Education", "School", listOf(TransactionType.EXPENSE)),
    TRAVEL("Travel", "Flight", listOf(TransactionType.EXPENSE)),
    INVESTMENTS("Investments", "TrendingUp", listOf(TransactionType.INCOME)),
    INTEREST("Interest", "Percent", listOf(TransactionType.INCOME)),
    REFUND("Refund", "SettingsBackupRestore", listOf(TransactionType.INCOME)),
    GIFTS("Gifts", "CardGiftcard", listOf(TransactionType.INCOME, TransactionType.EXPENSE)),
    INSURANCE("Insurance", "Shield", listOf(TransactionType.INCOME, TransactionType.EXPENSE)),
    OTHERS("Others", "MoreHoriz", listOf(TransactionType.INCOME, TransactionType.EXPENSE));

    val icon: ImageVector
        get() = when (iconName) {
            "AccountBalance" -> Icons.Rounded.AccountBalance
            "Restaurant" -> Icons.Rounded.Restaurant
            "ShoppingCart" -> Icons.Rounded.ShoppingCart
            "ShoppingBag" -> Icons.Rounded.ShoppingBag
            "DirectionsCar" -> Icons.Rounded.DirectionsCar
            "Home" -> Icons.Rounded.Home
            "Lightbulb" -> Icons.Rounded.Lightbulb
            "Movie" -> Icons.Rounded.Movie
            "MedicalServices" -> Icons.Rounded.MedicalServices
            "School" -> Icons.Rounded.School
            "Flight" -> Icons.Rounded.Flight
            "TrendingUp" -> Icons.AutoMirrored.Rounded.TrendingUp
            "Percent" -> Icons.Rounded.Percent
            "SettingsBackupRestore" -> Icons.Rounded.SettingsBackupRestore
            "CardGiftcard" -> Icons.Rounded.CardGiftcard
            "Shield" -> Icons.Rounded.Shield
            else -> Icons.Rounded.MoreHoriz
        }

    val iconBgColor: Color
        get() = when (this) {
            SALARY -> Color(0xFFD1EDFF)        // Light Blue
            FOOD -> Color(0xFFFFD6D6)          // Light Red
            GROCERIES -> Color(0xFFE1F5FE)      // Light Sky Blue
            SHOPPING -> Color(0xFFE8F5E9)      // Light Green
            TRANSPORT -> Color(0xFFFFF9C4)     // Light Yellow
            HOUSING -> Color(0xFFEDE7F6)       // Light Lavender
            UTILITIES -> Color(0xFFFFF3E0)     // Light Orange
            ENTERTAINMENT -> Color(0xFFFCE4EC) // Light Pink
            HEALTH -> Color(0xFFE0F2F1)        // Light Teal
            EDUCATION -> Color(0xFFF1F8E9)     // Light Lime Green
            TRAVEL -> Color(0xFFE0F7FA)        // Light Cyan
            INVESTMENTS -> Color(0xFFE8F5E9)   // Light Mint Green
            INTEREST -> Color(0xFFFFF9C4)      // Light Yellow
            REFUND -> Color(0xFFE0F7FA)         // Light Cyan
            GIFTS -> Color(0xFFFCE4EC)          // Light Pink
            INSURANCE -> Color(0xFFE0F2F1)      // Light Teal
            OTHERS -> Color(0xFFF3F4F6)        // Light Gray
        }

    companion object {
        fun fromString(value: String): TransactionCategory {
            return try {
                valueOf(value.uppercase())
            } catch (e: Exception) {
                when (value.lowercase()) {
                    "salary", "income" -> SALARY
                    "food", "dining" -> FOOD
                    "groceries" -> GROCERIES
                    "shopping", "store" -> SHOPPING
                    "transport", "taxi", "uber" -> TRANSPORT
                    "housing", "rent", "mortgage" -> HOUSING
                    "utilities", "bills" -> UTILITIES
                    "entertainment", "movies", "leisure" -> ENTERTAINMENT
                    "health", "medical", "fitness" -> HEALTH
                    "education" -> EDUCATION
                    "travel" -> TRAVEL
                    "investments", "investment" -> INVESTMENTS
                    "interest" -> INTEREST
                    "refund" -> REFUND
                    "gifts", "gift" -> GIFTS
                    "insurance" -> INSURANCE
                    else -> OTHERS
                }
            }
        }
    }
}
