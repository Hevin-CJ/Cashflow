package com.hevincj.cashflow.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

object ApkInstaller {

    private const val TAG = "ApkInstaller"
    const val APK_MIME_TYPE = "application/vnd.android.package-archive"

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun createPermissionSettingsIntent(packageName: String): Intent {
        return Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = createPermissionSettingsIntent(context.packageName)
            context.startActivity(intent)
        }
    }

    /**
     * Creates an Intent to install the given APK URI via system Package Installer.
     */
    fun createInstallIntent(context: Context, apkUri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    /**
     * Resolves the content URI for an APK file using FileProvider.
     */
    fun getApkUri(context: Context, apkFile: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )
    }

    /**
     * Installs the given APK file.
     * Launches the system PackageInstaller Activity directly from the foreground UI context
     * using Intent.ACTION_VIEW and FileProvider URI. This guarantees immediate response across
     * all Android versions (Android 8 to 16+) without being blocked by Background Activity Launch (BAL) restrictions.
     */
    fun installApk(context: Context, apkFile: File): Result<Unit> {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            return Result.failure(IllegalArgumentException("APK file does not exist or is empty."))
        }

        return installViaActionView(context, apkFile)
    }

    fun installViaActionView(context: Context, apkFile: File): Result<Unit> {
        return try {
            val apkUri = getApkUri(context, apkFile)
            val intent = createInstallIntent(context, apkUri)

            context.startActivity(intent)
            CrashLogger.i(TAG, "Launched PackageInstaller Activity for APK: ${apkFile.name} (${apkFile.length()} bytes)")
            Result.success(Unit)
        } catch (e: Exception) {
            CrashLogger.e(TAG, "Failed to launch ACTION_VIEW for APK install: ${e.message}", e)
            Result.failure(e)
        }
    }
}

