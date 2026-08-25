package com.hevincj.cashflow.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast

object FileOpener {

    /**
     * Returns the ordered candidate MIME types to query and attempt when opening a file.
     */
    fun getCandidateMimeTypes(primaryMimeType: String): List<String> {
        return when (primaryMimeType) {
            "text/csv" -> listOf(
                "text/csv",
                "text/comma-separated-values",
                "application/vnd.ms-excel",
                "application/csv",
                "application/x-csv",
                "text/plain",
                "text/*",
                "*/*"
            )
            "application/pdf" -> listOf(
                "application/pdf",
                "*/*"
            )
            else -> listOf(primaryMimeType, "*/*")
        }
    }

    /**
     * Builds an ACTION_VIEW Intent for a given URI and candidate MIME type.
     */
    fun buildViewIntent(uri: Uri, mimeType: String): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Builds an ACTION_SEND Intent (Share/Save to external app) as a fallback.
     */
    fun buildShareIntent(uri: Uri, primaryMimeType: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = if (primaryMimeType == "text/csv") "text/*" else primaryMimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Queries package manager across all candidate MIME types to collect unique viewer intents.
     */
    fun resolveMatchingViewerIntents(
        context: Context,
        uri: Uri,
        primaryMimeType: String
    ): List<Intent> {
        val packageManager = context.packageManager ?: return emptyList()
        val candidates = getCandidateMimeTypes(primaryMimeType)

        val matchingIntents = mutableListOf<Intent>()
        val seenPackages = mutableSetOf<String>()

        for (mime in candidates) {
            val intent = buildViewIntent(uri, mime)
            val resolvedList = try {
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            } catch (e: Exception) {
                CrashLogger.w("FileOpener", "queryIntentActivities error for MIME $mime: ${e.message}", e)
                emptyList()
            }

            for (info in resolvedList) {
                val activityInfo = info.activityInfo ?: continue
                val pkgName = activityInfo.packageName ?: continue

                // Exclude self from handling the view intent
                if (pkgName != context.packageName && pkgName !in seenPackages) {
                    seenPackages.add(pkgName)
                    val targetedIntent = Intent(intent).apply {
                        `package` = pkgName
                    }
                    matchingIntents.add(targetedIntent)
                }
            }
        }
        return matchingIntents
    }

    /**
     * Resiliently opens a saved file URI by querying and aggregating all available viewing apps
     * across candidate MIME types (spreadsheets, file managers, text editors, office apps) into
     * a unified system chooser, falling back to ACTION_SEND if no direct viewer is available.
     */
    fun openFile(context: Context, uri: Uri, primaryMimeType: String) {
        val matchingIntents = resolveMatchingViewerIntents(context, uri, primaryMimeType).toMutableList()

        if (matchingIntents.isNotEmpty()) {
            try {
                val baseIntent = matchingIntents.removeAt(0)
                val chooserTitle = if (primaryMimeType == "text/csv") "Open CSV with" else "Open file with"
                val chooser = Intent.createChooser(baseIntent, chooserTitle)?.apply {
                    if (matchingIntents.isNotEmpty()) {
                        putExtra(Intent.EXTRA_INITIAL_INTENTS, matchingIntents.toTypedArray())
                    }
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (chooser != null) {
                    context.startActivity(chooser)
                    return
                } else {
                    context.startActivity(baseIntent)
                    return
                }
            } catch (e: Exception) {
                CrashLogger.w("FileOpener", "Failed launching aggregated chooser: ${e.message}", e)
            }
        }

        // 2. Fallback to ACTION_SEND (Share sheet / Save to File Manager, Drive, etc.)
        try {
            val shareIntent = buildShareIntent(uri, primaryMimeType)
            val chooser = Intent.createChooser(shareIntent, "Open or share file")?.apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (chooser != null) {
                context.startActivity(chooser)
            } else {
                context.startActivity(shareIntent)
            }
        } catch (e: Exception) {
            CrashLogger.e("FileOpener", "All attempts to open or share file failed", e)
            Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }
}
