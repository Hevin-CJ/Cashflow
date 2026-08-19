package com.hevincj.cashflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hevincj.cashflow.domain.models.UserProfile

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val username: String,
    val firstName: String?,
    val lastName: String?,
    val phoneNumber: String?,
    val profileImage: String?,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toDomain(): UserProfile {
        return UserProfile(
            username = username,
            firstName = firstName,
            lastName = lastName,
            phoneNumber = phoneNumber,
            profileImage = profileImage
        )
    }

    companion object {
        fun fromDomain(profile: UserProfile): UserProfileEntity {
            return UserProfileEntity(
                id = 1,
                username = profile.username,
                firstName = profile.firstName,
                lastName = profile.lastName,
                phoneNumber = profile.phoneNumber,
                profileImage = profile.profileImage
            )
        }
    }
}
