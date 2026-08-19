package com.hevincj.cashflow.domain.repository

import com.hevincj.cashflow.domain.models.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserProfileFlow(): Flow<UserProfile?>
    suspend fun getUserProfile(): Result<UserProfile>
    suspend fun updateProfile(
        firstName: String?,
        lastName: String?,
        phoneNumber: String?,
        profileImage: String?
    ): Result<UserProfile>
}
