package com.hevincj.cashflow.data.repository

import com.hevincj.cashflow.data.remote.api.UserApi
import com.hevincj.cashflow.data.remote.models.UpdateProfileRequestDto
import com.hevincj.cashflow.domain.models.UserProfile
import com.hevincj.cashflow.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val api: UserApi
) : UserRepository {

    override suspend fun getUserProfile(): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val response = api.getUserProfile()
            if (response.isSuccessful) {
                val body = response.body() ?: throw Exception("Empty response body")
                Result.success(
                    UserProfile(
                        username = body.username,
                        firstName = body.firstName,
                        lastName = body.lastName,
                        phoneNumber = body.phoneNumber,
                        profileImage = body.profileImage
                    )
                )
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
                Result.success(
                    UserProfile(
                        username = body.username,
                        firstName = body.firstName,
                        lastName = body.lastName,
                        phoneNumber = body.phoneNumber,
                        profileImage = body.profileImage
                    )
                )
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
