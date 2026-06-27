package com.hevincj.cashflow.data.repository

import com.hevincj.cashflow.data.local.dao.RecurringExpenseDao
import com.hevincj.cashflow.data.local.entity.RecurringExpenseEntity
import com.hevincj.cashflow.data.local.PendingDeleteManager
import com.hevincj.cashflow.data.remote.api.RecurringExpenseApi
import com.hevincj.cashflow.data.remote.models.RecurringExpenseDto
import com.hevincj.cashflow.data.worker.RecurringExpenseSyncScheduler
import com.hevincj.cashflow.data.worker.RecurringExpenseSyncManager
import com.hevincj.cashflow.data.mapper.toDomain
import com.hevincj.cashflow.data.mapper.toEntity
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.models.RecurringFrequency
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringExpenseRepositoryImplTest {

    @Mock
    lateinit var dao: RecurringExpenseDao

    @Mock
    lateinit var api: RecurringExpenseApi

    @Mock
    lateinit var syncScheduler: RecurringExpenseSyncScheduler

    @Mock
    lateinit var pendingDeleteManager: PendingDeleteManager

    @Mock
    lateinit var syncManager: RecurringExpenseSyncManager

    private lateinit var repository: RecurringExpenseRepositoryImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        repository = RecurringExpenseRepositoryImpl(dao, api, syncScheduler, pendingDeleteManager, syncManager)
    }

    private fun createEntity(
        id: Int,
        serverId: String?,
        isSynced: Boolean = true,
        nextDueDate: Long,
        lastProcessedDate: Long? = null
    ): RecurringExpenseEntity {
        return RecurringExpenseEntity(
            id = id,
            serverId = serverId,
            isSynced = isSynced,
            title = "Subscription",
            amount = 9.99,
            category = TransactionCategory.OTHERS,
            type = TransactionType.EXPENSE,
            frequency = RecurringFrequency.MONTHLY,
            startDate = 1000L,
            lastProcessedDate = lastProcessedDate,
            nextDueDate = nextDueDate,
            description = "Test Sub"
        )
    }

    private fun createDto(
        id: String,
        nextDueDate: Long,
        lastProcessedDate: Long? = null
    ): RecurringExpenseDto {
        return RecurringExpenseDto(
            id = id,
            userId = "user_1",
            title = "Subscription",
            amount = 9.99,
            category = "SERVICES",
            type = "EXPENSE",
            frequency = "MONTHLY",
            startDate = 1000L,
            lastProcessedDate = lastProcessedDate,
            nextDueDate = nextDueDate,
            description = "Test Sub"
        )
    }

    @Test
    fun testSyncRetainsLatestNextDueDate() = runTest {
        // Local state has an advanced nextDueDate because it was already processed locally.
        // e.g., local nextDueDate = 3000L, but server still has 2000L.
        val localEntity = createEntity(
            id = 42,
            serverId = "server_sub_1",
            isSynced = true,
            nextDueDate = 3000L,
            lastProcessedDate = 2000L
        )
        whenever(dao.getActiveRecurringExpensesList()).thenReturn(listOf(localEntity))
        whenever(dao.getUnsyncedRecurringExpenses()).thenReturn(emptyList())
        whenever(pendingDeleteManager.getPendingRecurringDeletions()).thenReturn(emptySet())

        // Remote response has outdated/stale dates
        val serverDto = createDto(
            id = "server_sub_1",
            nextDueDate = 2000L,
            lastProcessedDate = 1000L
        )
        whenever(api.getRecurringExpenses()).thenReturn(Response.success(listOf(serverDto)))

        val result = repository.syncRecurringExpenses()
        assertNull(result)

        // Capture what was sent to refreshSyncedRecurringExpenses
        val insertCaptor = argumentCaptor<List<RecurringExpenseEntity>>()
        verify(dao).refreshSyncedRecurringExpenses(any(), insertCaptor.capture())

        val inserted = insertCaptor.firstValue
        assertEquals(1, inserted.size)
        // Verify it kept the local/advanced nextDueDate (3000L) and lastProcessedDate (2000L)
        assertEquals(3000L, inserted[0].nextDueDate)
        assertEquals(2000L, inserted[0].lastProcessedDate)
        assertEquals(42, inserted[0].id)
    }

    @Test
    fun testSyncAcceptsServerNextDueDateIfNewer() = runTest {
        // Local state is outdated/behind the server.
        // e.g., local nextDueDate = 2000L, server nextDueDate = 3000L.
        val localEntity = createEntity(
            id = 42,
            serverId = "server_sub_1",
            isSynced = true,
            nextDueDate = 2000L,
            lastProcessedDate = 1000L
        )
        whenever(dao.getActiveRecurringExpensesList()).thenReturn(listOf(localEntity))
        whenever(dao.getUnsyncedRecurringExpenses()).thenReturn(emptyList())
        whenever(pendingDeleteManager.getPendingRecurringDeletions()).thenReturn(emptySet())

        // Remote response has the newer/advanced dates
        val serverDto = createDto(
            id = "server_sub_1",
            nextDueDate = 3000L,
            lastProcessedDate = 2000L
        )
        whenever(api.getRecurringExpenses()).thenReturn(Response.success(listOf(serverDto)))

        val result = repository.syncRecurringExpenses()
        assertNull(result)

        val insertCaptor = argumentCaptor<List<RecurringExpenseEntity>>()
        verify(dao).refreshSyncedRecurringExpenses(any(), insertCaptor.capture())

        val inserted = insertCaptor.firstValue
        assertEquals(1, inserted.size)
        // Verify it accepted the server's newer nextDueDate (3000L) and lastProcessedDate (2000L)
        assertEquals(3000L, inserted[0].nextDueDate)
        assertEquals(2000L, inserted[0].lastProcessedDate)
        assertEquals(42, inserted[0].id)
    }

    @Test
    fun testUpdateRecurringExpenseResolvesLocalId() = runTest {
        val existingEntity = createEntity(
            id = 42,
            serverId = "server_sub_1",
            isSynced = true,
            nextDueDate = 2000L
        )
        whenever(dao.getRecurringExpenseByServerId("server_sub_1")).thenReturn(existingEntity)

        val updatedDomainModel = existingEntity.toDomain().copy(
            nextDueDate = 3000L
        )

        repository.updateRecurringExpense(updatedDomainModel)

        val updateCaptor = argumentCaptor<RecurringExpenseEntity>()
        verify(dao).updateRecurringExpense(updateCaptor.capture())

        val updatedEntity = updateCaptor.firstValue
        assertEquals(42, updatedEntity.id)
        assertEquals(3000L, updatedEntity.nextDueDate)
        verify(syncScheduler).scheduleUpsertSync(42)
    }
}

