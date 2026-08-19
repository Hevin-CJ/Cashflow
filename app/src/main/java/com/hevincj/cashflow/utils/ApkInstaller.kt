package com.hevincj.cashflow.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream

object ApkInstaller {

    private const val TAG = "ApkInstaller"

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Installs the given APK file.
     * Uses session-based PackageInstaller on Android 12+ (API 31+) to enable seamless self-updates
     * without triggering Play Protect sideloading / biometric lock-screen prompts.
     * Falls back to Intent.ACTION_VIEW if session install fails or on older Android versions.
     */
    fun installApk(context: Context, apkFile: File): Result<Unit> {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            return Result.failure(IllegalArgumentException("APK file does not exist or is empty."))
        }

        // Try modern session-based PackageInstaller on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && canRequestPackageInstalls(context)) {
            val sessionResult = installViaPackageInstallerSession(context, apkFile)
            if (sessionResult.isSuccess) {
                return sessionResult
            }
            Log.w(TAG, "PackageInstaller session failed, falling back to ACTION_VIEW", sessionResult.exceptionOrNull())
        }

        // Fallback to legacy Intent.ACTION_VIEW
        return installViaActionView(context, apkFile)
    }

    private fun installViaPackageInstallerSession(context: Context, apkFile: File): Result<Unit> {
        return try {
            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(context.packageName)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    setInstallReason(PackageManager.INSTALL_REASON_USER)
                }
            }

            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            session.use { activeSession ->
                FileInputStream(apkFile).use { input ->
                    activeSession.openWrite("cashflow_update", 0, apkFile.length()).use { output ->
                        input.copyTo(output)
                        activeSession.fsync(output)
                    }
                }

                val callbackIntent = Intent(context, UpdateInstallReceiver::class.java).apply {
                    action = UpdateInstallReceiver.ACTION_INSTALL_COMPLETE
                }

                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    callbackIntent,
                    flags
                )

                activeSession.commit(pendingIntent.intentSender)
            }

            Log.d(TAG, "PackageInstaller session #$sessionId committed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun installViaActionView(context: Context, apkFile: File): Result<Unit> {
        return try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
