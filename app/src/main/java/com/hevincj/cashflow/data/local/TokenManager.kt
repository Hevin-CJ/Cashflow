package com.hevincj.cashflow.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        try {
            prefs.edit { putString("jwt_token", token) }
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.e("TokenManager", "Failed to save JWT token", e)
        }
    }

    fun getToken(): String? {
        return try {
            prefs.getString("jwt_token", null)
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.e("TokenManager", "Failed to read JWT token", e)
            null
        }
    }

    fun clearToken() {
        try {
            prefs.edit { remove("jwt_token") }
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.e("TokenManager", "Failed to clear JWT token", e)
        }
    }
}