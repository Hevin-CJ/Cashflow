package com.hevincj.cashflow.utils

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hevincj.cashflow.BuildConfig

/**
 * Centralized logging and diagnostic reporting utility for CashFlow.
 *
 * In Debug builds: Outputs to Android Logcat.
 * In Release builds: Drops verbose/debug logs, streams info/warnings as Crashlytics
 * breadcrumbs, and reports non-fatal exceptions to Firebase Crashlytics.
 */
object CrashLogger {

    private fun getCrashlyticsSafe(): FirebaseCrashlytics? {
        return try {
            FirebaseCrashlytics.getInstance()
        } catch (_: Throwable) {
            null
        }
    }

    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message)
        }
        try {
            getCrashlyticsSafe()?.log("[$tag] $message")
        } catch (_: Throwable) {}
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (throwable != null) {
                Log.w(tag, message, throwable)
            } else {
                Log.w(tag, message)
            }
        }
        try {
            val crashlytics = getCrashlyticsSafe()
            val logMessage = if (throwable != null) "[$tag] WARN: $message - ${throwable.message}" else "[$tag] WARN: $message"
            crashlytics?.log(logMessage)
        } catch (_: Throwable) {}
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
        try {
            val crashlytics = getCrashlyticsSafe()
            crashlytics?.log("[$tag] ERROR: $message")
            val targetException = throwable ?: Exception("[$tag] $message")
            crashlytics?.recordException(targetException)
        } catch (_: Throwable) {}
    }

    fun recordException(throwable: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.e("CrashLogger", "Recorded non-fatal exception", throwable)
        }
        try {
            getCrashlyticsSafe()?.recordException(throwable)
        } catch (_: Throwable) {}
    }

    fun setUserId(userId: String) {
        try {
            getCrashlyticsSafe()?.setUserId(userId)
        } catch (_: Throwable) {}
    }

    fun setCustomKey(key: String, value: String) {
        try {
            getCrashlyticsSafe()?.setCustomKey(key, value)
        } catch (_: Throwable) {}
    }

    fun setCustomKey(key: String, value: Boolean) {
        try {
            getCrashlyticsSafe()?.setCustomKey(key, value)
        } catch (_: Throwable) {}
    }

    fun setCustomKey(key: String, value: Int) {
        try {
            getCrashlyticsSafe()?.setCustomKey(key, value)
        } catch (_: Throwable) {}
    }

    fun setCustomKey(key: String, value: Long) {
        try {
            getCrashlyticsSafe()?.setCustomKey(key, value)
        } catch (_: Throwable) {}
    }

    fun setCustomKey(key: String, value: Double) {
        try {
            getCrashlyticsSafe()?.setCustomKey(key, value)
        } catch (_: Throwable) {}
    }
}
