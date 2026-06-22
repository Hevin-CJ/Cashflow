package com.hevincj.cashflow.data.repository

import com.hevincj.cashflow.data.local.dao.TransactionDao
import com.hevincj.cashflow.data.local.entity.TransactionEntity
import com.hevincj.cashflow.data.local.PendingDeleteManager
import com.hevincj.cashflow.data.remote.api.TransactionApi
import com.hevincj.cashflow.data.remote.models.TransactionDto
import com.hevincj.cashflow.data.remote.models.TransactionRequestDto
import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.utils.NetworkMonitor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ShoppingBag
import com.hevincj.cashflow.data.worker.TransactionSyncManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionRepositoryImplTest {

    @Mock
    lateinit var dao: TransactionDao

    @Mock
    lateinit var api: TransactionApi

    @Mock
    lateinit var networkMonitor: NetworkMonitor

    @Mock
    lateinit var syncScheduler: com.hevincj.cashflow.data.worker.TransactionSyncScheduler

    @Mock
    lateinit var pendingDeleteManager: PendingDeleteManager

    @Mock
    lateinit var syncManager: TransactionSyncManager

    private lateinit var repository: TransactionRepositoryImpl

    private val sampleIcon = Icons.Rounded.ShoppingBag
    private val sampleColor = Color.Red

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        repository = TransactionRepositoryImpl(dao, api, syncScheduler, pendingDeleteManager, syncManager)
    }

    private fun createDomainTransaction(
        id: String,
        amount: Double,
        type: TransactionType,
        isSynced: Boolean = false
    ): Transaction {
        return Transaction(
            id = id,
            title = "Test",
            timestamp = 1000L,
            amount = amount,
            icon = sampleIcon,
            iconBgColor = sampleColor,
            type = type,
            category = TransactionCategory.GROCERIES,
            description = "desc",
            isSynced = isSynced
        )
    }

    private fun createEntityTransaction(
        id: Int,
        serverId: String?,
        amount: Double,
        type: String,
        isSynced: Boolean = false
    ): TransactionEntity {
        return TransactionEntity(
            id = id,
            serverId = serverId,
            title = "Test",
            timestamp = 1000L,
            amount = amount,
            iconName = "GROCERIES",
            iconBgColor = sampleColor.toArgb(),
            type = type,
            category = "GROCERIES",
            description = "desc",
            isSynced = isSynced
        )
    }

    @Test
    fun testGetAllTransactionsMapsEntitiesToDomain() = runTest {
        val entities = listOf(
            createEntityTransaction(1, "server_1", 100.0, "INCOME", true),
            createEntityTransaction(2, null, -50.0, "EXPENSE", false)
        )

        whenever(dao.getAllTransactions()).thenReturn(flowOf(entities))

        val result = repository.getAllTransactions().first()

        assertEquals(2, result.size)
        assertEquals("server_1", result[0].id)
        assertEquals(100.0, result[0].amount, 0.01)
        assertEquals(TransactionType.INCOME, result[0].type)
        assertEquals(true, result[0].isSynced)

        assertEquals("2", result[1].id)
        assertEquals(-50.0, result[1].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result[1].type)
        assertEquals(false, result[1].isSynced)
    }

    @Test
    fun testInsertTransactionOfflineFirstInsertsLocallyAndSchedulesSync() = runTest {
        val domainTransaction = createDomainTransaction("0", 100.0, TransactionType.INCOME, false)
        whenever(dao.getAllTransactions()).thenReturn(flowOf(emptyList()))
        whenever(dao.insertTransaction(any())).thenReturn(10L)

        repository.insertTransaction(domainTransaction)

        // Verify initial local insert only
        val captor = argumentCaptor<TransactionEntity>()
        verify(dao, times(1)).insertTransaction(captor.capture())

        val firstInsert = captor.firstValue
        assertEquals(0, firstInsert.id)
        assertEquals(false, firstInsert.isSynced)

        // Verify remote create call is NOT made inline
        verify(api, never()).createTransaction(any())
        verify(syncScheduler).scheduleUpsertSync(10)
    }

    @Test
    fun testInsertTransactionUnsyncedUpdatesLocallyOnly() = runTest {
        val domainTransaction = createDomainTransaction("server_123", 100.0, TransactionType.INCOME, false)
        val existingEntities = listOf(createEntityTransaction(10, "server_123", 100.0, "INCOME", false))
        whenever(dao.getAllTransactions()).thenReturn(flowOf(existingEntities))
        whenever(dao.insertTransaction(any())).thenReturn(10L)

        repository.insertTransaction(domainTransaction)

        val captor = argumentCaptor<TransactionEntity>()
        verify(dao, times(1)).insertTransaction(captor.capture())

        val firstInsert = captor.firstValue
        assertEquals(10, firstInsert.id)
        assertEquals("server_123", firstInsert.serverId)
        assertEquals(false, firstInsert.isSynced)

        verify(api, never()).updateTransaction(any(), any())
        verify(syncScheduler).scheduleUpsertSync(10)
    }


    @Test
    fun testDeleteTransactionDeletesLocallyByServerIdAndSchedulesDeleteSync() = runTest {
        val domainTransaction = createDomainTransaction("server_123", 100.0, TransactionType.INCOME, true)

        repository.deleteTransaction(domainTransaction)

        verify(dao).deleteByServerId("server_123")
        // Verify remote delete call is NOT made inline
        verify(api, never()).deleteTransaction(any())
        verify(syncScheduler).scheduleDeleteSync("server_123")
    }

    @Test
    fun testDeleteTransactionWithoutServerIdUsesIdAndFindsServerIdToSync() = runTest {
        val domainTransaction = createDomainTransaction("123", 100.0, TransactionType.INCOME, true)
        val existing = createEntityTransaction(123, "server_456", 100.0, "INCOME", true)
        whenever(dao.getAllTransactions()).thenReturn(flowOf(listOf(existing)))

        repository.deleteTransaction(domainTransaction)

        verify(dao).deleteById(123)
        // Verify remote delete call is NOT made inline
        verify(api, never()).deleteTransaction(any())
        verify(syncScheduler).scheduleDeleteSync("server_456")
    }

    @Test
    fun testSyncTransactionsFetchesRemoteAndDoesNotPushInline() = runTest {
        // Mock transaction locally
        val synced = createEntityTransaction(11, "server_old", 100.0, "INCOME", true)
        whenever(dao.getAllTransactions()).thenReturn(flowOf(listOf(synced)))

        // Mock remote fetch success
        val remoteDtos = listOf(
            TransactionDto("server_old", "user", 100.0, "INCOME", "GROCERIES", "desc", 1000L),
            TransactionDto("server_new", "user", 50.0, "INCOME", "GROCERIES", "desc", 1000L)
        )
        whenever(api.getTransactions(any(), any())).thenReturn(Response.success(remoteDtos))

        val result = repository.syncTransactions()

        assertNull(result)

        // Verify remote fetch logic gets called
        verify(api).getTransactions(page = 1, limit = 1000)
        verify(api, never()).createTransaction(any())

        // Verify local refresh is triggered
        val deleteCaptor = argumentCaptor<List<TransactionEntity>>()
        val insertCaptor = argumentCaptor<List<TransactionEntity>>()
        verify(dao).refreshSyncedTransactions(deleteCaptor.capture(), insertCaptor.capture())

        // delete list should be empty (nothing deleted on server)
        assertEquals(0, deleteCaptor.firstValue.size)
        // insert list should have both remote items mapped to local
        assertEquals(2, insertCaptor.firstValue.size)
    }

    @Test
    fun testSyncTransactionsDeletesOrphansLocally() = runTest {
        // Mock local synced transaction that was deleted on server
        // Timestamp 1500L, serverId = "server_deleted"
        val syncedDeleted = createEntityTransaction(11, "server_deleted", 100.0, "INCOME", true).copy(timestamp = 1500L)
        
        // Return this inside getAllTransactions
        whenever(dao.getAllTransactions()).thenReturn(flowOf(listOf(syncedDeleted)))

        // Remote fetch returns a transaction with timestamp 1500L, which is the oldest remote timestamp
        val remoteDtos = listOf(
            TransactionDto("server_active", "user", 50.0, "INCOME", "GROCERIES", "desc", 1500L)
        )
        whenever(api.getTransactions(any(), any())).thenReturn(Response.success(remoteDtos))

        val result = repository.syncTransactions()

        assertNull(result)

        val deleteCaptor = argumentCaptor<List<TransactionEntity>>()
        val insertCaptor = argumentCaptor<List<TransactionEntity>>()
        verify(dao).refreshSyncedTransactions(deleteCaptor.capture(), insertCaptor.capture())

        // Should detect that "server_deleted" is missing from remote response but has timestamp >= 1500L, so delete it
        assertEquals(1, deleteCaptor.firstValue.size)
        assertEquals("server_deleted", deleteCaptor.firstValue[0].serverId)

        assertEquals(1, insertCaptor.firstValue.size)
        assertEquals("server_active", insertCaptor.firstValue[0].serverId)
    }

    @Test
    fun testSyncTransactionsUnknownHostExceptionReturnsNoInternet() = runTest {
        whenever(dao.getAllTransactions()).thenReturn(flowOf(emptyList()))
        whenever(api.getTransactions(any(), any())).thenAnswer { throw UnknownHostException() }
        assertEquals("No internet connection", repository.syncTransactions())
    }

    @Test
    fun testSyncTransactionsConnectExceptionReturnsFailedToConnect() = runTest {
        whenever(dao.getAllTransactions()).thenReturn(flowOf(emptyList()))
        whenever(api.getTransactions(any(), any())).thenAnswer { throw ConnectException() }
        assertEquals("Failed to connect to the server.", repository.syncTransactions())
    }

    @Test
    fun testSyncTransactionsSocketTimeoutExceptionReturnsTimeout() = runTest {
        whenever(dao.getAllTransactions()).thenReturn(flowOf(emptyList()))
        whenever(api.getTransactions(any(), any())).thenAnswer { throw SocketTimeoutException() }
        assertEquals("Connection timed out.", repository.syncTransactions())
    }

    @Test
    fun testSyncTransactionsIOExceptionReturnsUnreachable() = runTest {
        whenever(dao.getAllTransactions()).thenReturn(flowOf(emptyList()))
        whenever(api.getTransactions(any(), any())).thenAnswer { throw IOException() }
        assertEquals("Server is unreachable. Please try again later.", repository.syncTransactions())
    }

    @Test
    fun testSyncTransactionsInlineSyncsThenMatchesById() = runTest {
        // Unsynced local entity (id = 12, serverId = null)
        val unsyncedLocal = createEntityTransaction(12, null, -50.0, "EXPENSE", false)
        
        // Synced version after inline sync
        val syncedLocal = unsyncedLocal.copy(serverId = "server_matched", isSynced = true)
        
        // Mock getAllTransactions to return unsynced first, then synced
        whenever(dao.getAllTransactions())
            .thenReturn(flowOf(listOf(unsyncedLocal)))
            .thenReturn(flowOf(listOf(syncedLocal)))
        
        // Stub the syncManager delegator
        whenever(syncManager.syncSpecificTransaction(any(), any(), any())).thenReturn(true)
        
        // Mock getTransactions for the second step
        val remoteDto = TransactionDto("server_matched", "user", 50.0, "EXPENSE", "GROCERIES", "desc", 1000L)
        val remoteDtos = listOf(remoteDto)
        whenever(api.getTransactions(any(), any())).thenReturn(Response.success(remoteDtos))

        val result = repository.syncTransactions()

        assertNull(result)

        // Verify delegation to syncManager was made correctly
        verify(syncManager).syncSpecificTransaction("upsert", 12, null)

        // Capture what was refreshed
        val deleteCaptor = argumentCaptor<List<TransactionEntity>>()
        val insertCaptor = argumentCaptor<List<TransactionEntity>>()
        verify(dao).refreshSyncedTransactions(deleteCaptor.capture(), insertCaptor.capture())

        // Verify that it matched strictly by ID, preserved local ID (12), and refreshed
        assertEquals(0, deleteCaptor.firstValue.size)
        assertEquals(1, insertCaptor.firstValue.size)
        assertEquals(12, insertCaptor.firstValue[0].id)
        assertEquals("server_matched", insertCaptor.firstValue[0].serverId)
    }

}

