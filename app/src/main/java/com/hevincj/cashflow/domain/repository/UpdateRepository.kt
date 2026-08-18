package com.hevincj.cashflow.domain.repository

import com.hevincj.cashflow.domain.models.AppUpdateInfo

interface UpdateRepository {
    suspend fun checkForUpdate(currentVersionName: String): Result<AppUpdateInfo>
}
