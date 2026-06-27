package com.hevincj.cashflow.data.worker

import com.hevincj.cashflow.data.local.dao.TransactionDao
import com.hevincj.cashflow.data.local.dao.RecurringExpenseDao
import com.hevincj.cashflow.data.local.entity.TransactionEntity
import com.hevincj.cashflow.data.local.PendingDeleteManager
import com.hevincj.cashflow.data.remote.api.TransactionApi
import com.hevincj.cashflow.data.remote.models.TransactionDto
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.domain.models.TransactionCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionSyncManagerTest {

    @Mock
    lateinit var dao: TransactionDao

    @Mock
    lateinit var api: TransactionApi

    @Mock
    lateinit var pendingDeleteManager: PendingDeleteManager

    @Mock
    lateinit var recurringExpenseDao: RecurringExpenseDao

    @Mock
    lateinit var recurringExpenseSyncManager: RecurringExpenseSyncManager

    private lateinit var syncManager: TransactionSyncManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        syncManager = TransactionSyncManager(
            dao,
            recurringExpenseDao,
            { recurringExpenseSyncManager },
            api,
            pendingDeleteManager
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
            iconBgColor = 0xFFFF0000.toInt(),
            type = TransactionType.valueOf(type),
            category = TransactionCategory.GROCERIES,
            description = "desc",
            isSynced = isSynced
        )
    }

    @Test
    fun testSyncSpecificTransactionUpsertCreateSuccess() = runTest {
        val entity = createEntityTransaction(10, null, 100.0, "INCOME", false)
        whenever(dao.getTransactionById(10)).thenReturn(entity)
        whenever(dao.getAllTransactions()).thenReturn(flowOf(listOf(entity)))
        whenever(dao.insertTransaction(any())).thenReturn(10L)

        val responseDto = TransactionDto(
            id = "server_123",
            userId = "user_1",
            amount = 100.0,
            type = "INCOME",
            category = "GROCERIES",
            description = "desc",
            timestamp = 1000L
        )
        whenever(api.createTransaction(any())).thenReturn(Response.success(responseDto))

        val result = syncManager.syncSpecificTransaction("upsert", 10, null)

        assertNull(result)
        verify(api).createTransaction(any())
        val captor = argumentCaptor<TransactionEntity>()
        verify(dao).insertTransaction(captor.capture())
        val syncedInsert = captor.firstValue
        assertEquals(10, syncedInsert.id)
        assertEquals("server_123", syncedInsert.serverId)
        assertEquals(true, syncedInsert.isSynced)
        assertTrue(syncedInsert.lastModifiedLocal > 0)
    }

    @Test
    fun testSyncSpecificTransactionUpsertUpdateSuccess() = runTest {
        val entity = createEntityTransaction(10, "server_123", 100.0, "INCOME", false)
        whenever(dao.getTransactionById(10)).thenReturn(entity)
        whenever(dao.getAllTransactions()).thenReturn(flowOf(listOf(entity)))
        whenever(dao.insertTransaction(any())).thenReturn(10L)
        whenever(api.updateTransaction(eq("server_123"), any())).thenReturn(Response.success(Unit))

        val result = syncManager.syncSpecificTransaction("upsert", 10, "server_123")

        assertNull(result)
        verify(api).updateTransaction(eq("server_123"), any())
        val captor = argumentCaptor<TransactionEntity>()
        verify(dao).insertTransaction(captor.capture())
        val syncedInsert = captor.firstValue
        assertEquals(10, syncedInsert.id)
        assertEquals("server_123", syncedInsert.serverId)
        assertEquals(true, syncedInsert.isSynced)
        assertTrue(syncedInsert.lastModifiedLocal > 0)
    }

    @Test
    fun testSyncSpecificTransactionDeleteSuccess() = runTest {
        whenever(api.deleteTransaction(eq("server_123"))).thenReturn(Response.success(Unit))

        val result = syncManager.syncSpecificTransaction("delete", -1, "server_123")

        assertNull(result)
        verify(api).deleteTransaction(eq("server_123"))
    }

    @Test
    fun testSyncSpecificTransactionDeleteNotFoundReturnsTrue() = runTest {
        whenever(api.deleteTransaction(eq("server_123"))).thenReturn(Response.error(404, "".toResponseBody()))

        val result = syncManager.syncSpecificTransaction("delete", -1, "server_123")

        assertNull(result)
        verify(api).deleteTransaction(eq("server_123"))
    }

    @Test
    fun testSyncSpecificTransactionUpsertFailedReturnsFalse() = runTest {
        val entity = createEntityTransaction(10, null, 100.0, "INCOME", false)
        whenever(dao.getTransactionById(10)).thenReturn(entity)
        whenever(dao.getAllTransactions()).thenReturn(flowOf(listOf(entity)))
        whenever(api.createTransaction(any())).thenReturn(Response.error(500, "".toResponseBody()))

        val result = syncManager.syncSpecificTransaction("upsert", 10, null)

        assertNotNull(result)
        verify(api).createTransaction(any())
        verify(dao, never()).insertTransaction(any())
    }
}
