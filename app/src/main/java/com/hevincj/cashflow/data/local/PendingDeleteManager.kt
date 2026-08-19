package com.hevincj.cashflow.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingDeleteManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val sharedPrefs by lazy {
        context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
    }

    fun getPendingDeletions(): Set<String> {
        return try {
            sharedPrefs.getStringSet("pending_deletions", emptySet()) ?: emptySet()
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("PendingDeleteManager", "Failed to get pending deletions", e)
            emptySet()
        }
    }

    fun addPendingDeletion(serverId: String) {
        try {
            val current = getPendingDeletions().toMutableSet()
            current.add(serverId)
            sharedPrefs.edit().putStringSet("pending_deletions", current).apply()
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("PendingDeleteManager", "Failed to add pending deletion for $serverId", e)
        }
    }

    fun removePendingDeletion(serverId: String) {
        try {
            val current = getPendingDeletions().toMutableSet()
            current.remove(serverId)
            sharedPrefs.edit().putStringSet("pending_deletions", current).apply()
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("PendingDeleteManager", "Failed to remove pending deletion for $serverId", e)
        }
    }

    fun getPendingRecurringDeletions(): Set<String> {
        return try {
            sharedPrefs.getStringSet("pending_recurring_deletions", emptySet()) ?: emptySet()
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("PendingDeleteManager", "Failed to get pending recurring deletions", e)
            emptySet()
        }
    }

    fun addPendingRecurringDeletion(serverId: String) {
        try {
            val current = getPendingRecurringDeletions().toMutableSet()
            current.add(serverId)
            sharedPrefs.edit().putStringSet("pending_recurring_deletions", current).apply()
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("PendingDeleteManager", "Failed to add pending recurring deletion for $serverId", e)
        }
    }

    fun removePendingRecurringDeletion(serverId: String) {
        try {
            val current = getPendingRecurringDeletions().toMutableSet()
            current.remove(serverId)
            sharedPrefs.edit().putStringSet("pending_recurring_deletions", current).apply()
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("PendingDeleteManager", "Failed to remove pending recurring deletion for $serverId", e)
        }
    }

    fun getPendingBudgetDeletions(): Set<String> {
        return try {
            sharedPrefs.getStringSet("pending_budget_deletions", emptySet()) ?: emptySet()
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("PendingDeleteManager", "Failed to get pending budget deletions", e)
            emptySet()
        }
    }

    fun addPendingBudgetDeletion(serverId: String) {
        try {
            val current = getPendingBudgetDeletions().toMutableSet()
            current.add(serverId)
            sharedPrefs.edit().putStringSet("pending_budget_deletions", current).apply()
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("PendingDeleteManager", "Failed to add pending budget deletion for $serverId", e)
        }
    }

    fun removePendingBudgetDeletion(serverId: String) {
        try {
            val current = getPendingBudgetDeletions().toMutableSet()
            current.remove(serverId)
            sharedPrefs.edit().putStringSet("pending_budget_deletions", current).apply()
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("PendingDeleteManager", "Failed to remove pending budget deletion for $serverId", e)
        }
    }
}
