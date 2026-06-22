package com.hevincj.cashflow.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingDeleteManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPrefs by lazy {
        context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
    }

    fun getPendingDeletions(): Set<String> {
        return sharedPrefs.getStringSet("pending_deletions", emptySet()) ?: emptySet()
    }

    fun addPendingDeletion(serverId: String) {
        val current = getPendingDeletions().toMutableSet()
        current.add(serverId)
        sharedPrefs.edit().putStringSet("pending_deletions", current).apply()
    }

    fun removePendingDeletion(serverId: String) {
        val current = getPendingDeletions().toMutableSet()
        current.remove(serverId)
        sharedPrefs.edit().putStringSet("pending_deletions", current).apply()
    }
}
