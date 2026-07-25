package com.hevincj.cashflow.domain.repository

import com.hevincj.cashflow.domain.models.UserProfile

interface UserRepository {
    suspend fun getUserProfile(): Result<UserProfile>
    suspend fun updateProfile(
        firstName: String?,
        lastName: String?,
        phoneNumber: String?,
        profileImage: String?
    ): Result<UserProfile>
}
