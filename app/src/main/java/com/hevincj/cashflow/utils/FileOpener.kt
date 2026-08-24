package com.hevincj.cashflow.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
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
     * Resiliently opens a saved file URI by launching the Android system app chooser with
     * ACTION_VIEW and read URI permissions across candidate MIME types, falling back to
     * ACTION_SEND if no direct viewer is matched.
     */
    fun openFile(context: Context, uri: Uri, primaryMimeType: String) {
        val candidates = getCandidateMimeTypes(primaryMimeType)

        // 1. Try launching the system chooser with candidate MIME types directly
        for (candidate in candidates) {
            try {
                val viewIntent = buildViewIntent(uri, candidate)
                val chooserTitle = if (primaryMimeType == "text/csv") "Open CSV with" else "Open file with"
                val chooser = Intent.createChooser(viewIntent, chooserTitle).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                return
            } catch (e: ActivityNotFoundException) {
                CrashLogger.w("FileOpener", "No direct activity for candidate MIME: $candidate", e)
            } catch (e: Exception) {
                CrashLogger.w("FileOpener", "Failed opening candidate MIME: $candidate", e)
            }
        }

        // 2. Fallback to ACTION_SEND (Share sheet / Save to File Manager, Drive, etc.)
        try {
            val shareIntent = buildShareIntent(uri, primaryMimeType)
            val chooser = Intent.createChooser(shareIntent, "Open or share file").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            CrashLogger.e("FileOpener", "All attempts to open or share file failed", e)
            Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }
}
