package com.hevincj.cashflow.data.repository

import com.hevincj.cashflow.data.local.dao.UserProfileDao
import com.hevincj.cashflow.data.local.entity.UserProfileEntity
import com.hevincj.cashflow.data.remote.api.UserApi
import com.hevincj.cashflow.data.remote.models.UpdateProfileRequestDto
import com.hevincj.cashflow.domain.models.UserProfile
import com.hevincj.cashflow.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val api: UserApi,
    private val userProfileDao: UserProfileDao
) : UserRepository {

    override fun getUserProfileFlow(): Flow<UserProfile?> {
        return userProfileDao.getUserProfileFlow().map { it?.toDomain() }
    }

    override suspend fun getUserProfile(): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val response = api.getUserProfile()
            if (response.isSuccessful) {
                val body = response.body() ?: throw Exception("Empty response body")
                val profile = UserProfile(
                    username = body.username,
                    firstName = body.firstName,
                    lastName = body.lastName,
                    phoneNumber = body.phoneNumber,
                    profileImage = body.profileImage
                )
                userProfileDao.insertOrUpdateProfile(UserProfileEntity.fromDomain(profile))
                Result.success(profile)
            } else {
                val cached = userProfileDao.getUserProfile()
                if (cached != null) {
                    Result.success(cached.toDomain())
                } else {
                    val errorMsg = response.errorBody()?.string() ?: response.message()
                    com.hevincj.cashflow.utils.CrashLogger.w("UserRepository", "Get profile failed: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("UserRepository", "Exception getting user profile: ${e.message}", e)
            val cached = userProfileDao.getUserProfile()
            if (cached != null) {
                Result.success(cached.toDomain())
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateProfile(
        firstName: String?,
        lastName: String?,
        phoneNumber: String?,
        profileImage: String?
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val requestDto = UpdateProfileRequestDto(firstName, lastName, phoneNumber, profileImage)
            val response = api.updateProfile(requestDto)
            if (response.isSuccessful) {
                val body = response.body() ?: throw Exception("Empty response body")
                val profile = UserProfile(
                    username = body.username,
                    firstName = body.firstName,
                    lastName = body.lastName,
                    phoneNumber = body.phoneNumber,
                    profileImage = body.profileImage
                )
                userProfileDao.insertOrUpdateProfile(UserProfileEntity.fromDomain(profile))
                Result.success(profile)
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                com.hevincj.cashflow.utils.CrashLogger.w("UserRepository", "Update profile failed: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("UserRepository", "Exception updating profile: ${e.message}", e)
            Result.failure(e)
        }
    }
}
