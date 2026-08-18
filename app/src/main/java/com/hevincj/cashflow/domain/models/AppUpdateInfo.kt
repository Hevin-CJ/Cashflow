package com.hevincj.cashflow.domain.models

data class AppUpdateInfo(
    val isUpdateAvailable: Boolean,
    val latestVersion: String,
    val currentVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val apkSize: Long,
    val patchDownloadUrl: String? = null,
    val patchSize: Long? = null
) {
    val isDeltaPatch: Boolean
        get() = !patchDownloadUrl.isNullOrBlank()
}
