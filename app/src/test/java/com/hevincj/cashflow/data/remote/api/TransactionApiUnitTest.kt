package com.hevincj.cashflow.data.remote.api

import com.hevincj.cashflow.data.local.dao.TransactionDao
import com.hevincj.cashflow.data.local.PendingDeleteManager
import com.hevincj.cashflow.data.remote.models.TransactionDto
import com.hevincj.cashflow.data.repository.TransactionRepositoryImpl
import com.hevincj.cashflow.utils.NetworkMonitor
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response

import com.hevincj.cashflow.data.worker.TransactionSyncManager

class TransactionApiUnitTest {

    @Mock
    lateinit var transactionApi: TransactionApi

    @Mock
    lateinit var dao: TransactionDao

    @Mock
    lateinit var networkMonitor: NetworkMonitor

    @Mock
    lateinit var syncScheduler: com.hevincj.cashflow.data.worker.TransactionSyncScheduler

    @Mock
    lateinit var pendingDeleteManager: PendingDeleteManager

    @Mock
    lateinit var syncManager: TransactionSyncManager

    private lateinit var repository: TransactionRepositoryImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        repository = TransactionRepositoryImpl(dao, transactionApi, syncScheduler, pendingDeleteManager, syncManager)
    }

    @Test
    fun testGetTransactionsApiSuccess() {
        runBlocking {
            // Arrange: mock successful remote transactions response from server
            val remoteDto = TransactionDto(
                id = "server-id-123",
                userId = "user-123",
                amount = 1500.0,
                type = "INCOME",
                category = "SALARY",
                description = "Monthly stipend",
                timestamp = 1718278312000L
            )
            val expectedResponse = Response.success(listOf(remoteDto))
            
            whenever(transactionApi.getTransactions(any(), any())).thenReturn(expectedResponse)
            whenever(dao.getAllTransactions()).thenReturn(flowOf(emptyList()))

            // Act: Sync transactions
            val error = repository.syncTransactions(limit = 25)

            // Assert: Verify API is hit, and repository processes successfully (no error message returned)
            assertNull("Sync should succeed with no error message", error)
            verify(transactionApi).getTransactions(1, 25)
        }
    }

    @Test
    fun testGetTransactionsApiFailure() {
        runBlocking {
            // Arrange: mock server error response (e.g. 403 Forbidden)
            val errorResponseBody = "{\"message\":\"Forbidden\"}".toResponseBody(null)
            val expectedResponse = Response.error<List<TransactionDto>>(403, errorResponseBody)
            
            whenever(transactionApi.getTransactions(any(), any())).thenReturn(expectedResponse)
            whenever(dao.getAllTransactions()).thenReturn(flowOf(emptyList()))

            // Act: Sync transactions
            val error = repository.syncTransactions(limit = 25)

            // Assert: Verify error is caught and mapped to string
            assertEquals("Failed to sync: Response.error() (code 403)", error)
            verify(transactionApi).getTransactions(1, 25)
        }
    }
}
