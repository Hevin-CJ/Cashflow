package com.hevincj.cashflow.data.repository

import com.hevincj.cashflow.data.local.dao.UserProfileDao
import com.hevincj.cashflow.data.local.entity.UserProfileEntity
import com.hevincj.cashflow.data.remote.api.UserApi
import com.hevincj.cashflow.data.remote.models.UserProfileResponseDto
import com.hevincj.cashflow.domain.models.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response

class UserRepositoryImplTest {

    @Mock
    lateinit var userApi: UserApi

    @Mock
    lateinit var userProfileDao: UserProfileDao

    private lateinit var repository: UserRepositoryImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        repository = UserRepositoryImpl(userApi, userProfileDao)
    }

    @Test
    fun testGetUserProfileFlowMapsEntityToDomain() = runTest {
        val entity = UserProfileEntity(
            id = 1,
            username = "testuser",
            firstName = "John",
            lastName = "Doe",
            phoneNumber = "+1234567890",
            profileImage = "base64ImageData"
        )
        whenever(userProfileDao.getUserProfileFlow()).thenReturn(flowOf(entity))

        val result = repository.getUserProfileFlow().first()
        assertNotNull(result)
        assertEquals("testuser", result?.username)
        assertEquals("John", result?.firstName)
        assertEquals("Doe", result?.lastName)
        assertEquals("+1234567890", result?.phoneNumber)
        assertEquals("base64ImageData", result?.profileImage)
    }

    @Test
    fun testGetUserProfileSuccessCachesToRoom() = runTest {
        val dto = UserProfileResponseDto(
            username = "testuser",
            firstName = "John",
            lastName = "Doe",
            phoneNumber = "+1234567890",
            profileImage = "base64ImageData"
        )
        whenever(userApi.getUserProfile()).thenReturn(Response.success(dto))

        val result = repository.getUserProfile()
        assertTrue(result.isSuccess)
        assertEquals("testuser", result.getOrNull()?.username)
        verify(userProfileDao).insertOrUpdateProfile(any())
    }

    @Test
    fun testGetUserProfileNetworkFailureFallsBackToRoomCache() = runTest {
        whenever(userApi.getUserProfile()).thenThrow(RuntimeException("Network offline"))
        val cachedEntity = UserProfileEntity(
            id = 1,
            username = "cacheduser",
            firstName = "Jane",
            lastName = "Doe",
            phoneNumber = "+9876543210",
            profileImage = null
        )
        whenever(userProfileDao.getUserProfile()).thenReturn(cachedEntity)

        val result = repository.getUserProfile()
        assertTrue(result.isSuccess)
        assertEquals("cacheduser", result.getOrNull()?.username)
        assertEquals("Jane", result.getOrNull()?.firstName)
    }
}
