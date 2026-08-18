package com.hevincj.cashflow.ui.screen.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.hevincj.cashflow.MainDispatcherRule
import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.domain.usecase.AddTransactionUseCase
import com.hevincj.cashflow.domain.usecase.GetTransactionsUseCase
import com.hevincj.cashflow.domain.usecase.GetTransactionByIdUseCase
import com.hevincj.cashflow.domain.usecase.UpdateTransactionUseCase
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ShoppingBag
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    lateinit var addTransactionUseCase: AddTransactionUseCase

    @Mock
    lateinit var updateTransactionUseCase: UpdateTransactionUseCase

    @Mock
    lateinit var addRecurringExpenseUseCase: com.hevincj.cashflow.domain.usecase.AddRecurringExpenseUseCase

    @Mock
    lateinit var deleteRecurringExpenseUseCase: com.hevincj.cashflow.domain.usecase.DeleteRecurringExpenseUseCase

    @Mock
    lateinit var recurringExpenseRepository: com.hevincj.cashflow.domain.repository.RecurringExpenseRepository

    @Mock
    lateinit var getTransactionsUseCase: GetTransactionsUseCase

    @Mock
    lateinit var getTransactionByIdUseCase: GetTransactionByIdUseCase

    private lateinit var viewModel: AddTransactionViewModel

    private val sampleTransactions = listOf(
        Transaction(
            id = "123",
            title = "Groceries",
            timestamp = 1718278312000L,
            amount = -900.0,
            icon = androidx.compose.material.icons.Icons.Rounded.ShoppingBag,
            iconBgColor = Color(0xFFF19E79),
            type = TransactionType.EXPENSE,
            category = TransactionCategory.GROCERIES,
            description = "Week food shopping",
            isSynced = true
        ),
        Transaction(
            id = "456",
            title = "Salary",
            timestamp = 1718278312000L,
            amount = 5000.0,
            icon = androidx.compose.material.icons.Icons.Rounded.ShoppingBag,
            iconBgColor = Color(0xFF67E2AE),
            type = TransactionType.INCOME,
            category = TransactionCategory.SALARY,
            description = "Monthly pay",
            isSynced = true
        ),
        Transaction(
            id = "789",
            title = "Over Limit Groceries",
            timestamp = 1718278312000L,
            amount = -123456789.999,
            icon = androidx.compose.material.icons.Icons.Rounded.ShoppingBag,
            iconBgColor = Color(0xFFF19E79),
            type = TransactionType.EXPENSE,
            category = TransactionCategory.GROCERIES,
            description = "Over the limit and extra decimals",
            isSynced = true
        )
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        runBlocking {
            whenever(getTransactionsUseCase.invoke()).thenReturn(flowOf(sampleTransactions))
            whenever(recurringExpenseRepository.getActiveRecurringExpenses()).thenReturn(emptyList())
            whenever(getTransactionByIdUseCase.invoke(any())).thenAnswer { invocation ->
                val id = invocation.getArgument<String>(0)
                sampleTransactions.find { it.id == id }
            }
        }
    }

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()): AddTransactionViewModel {
        return AddTransactionViewModel(
            addTransactionUseCase,
            updateTransactionUseCase,
            addRecurringExpenseUseCase,
            deleteRecurringExpenseUseCase,
            recurringExpenseRepository,
            getTransactionsUseCase,
            getTransactionByIdUseCase,
            savedStateHandle
        )
    }

    @Test
    fun testInitializationWithoutIdStartsInCreationMode() {
        viewModel = createViewModel()

        val state = viewModel.state.value
        assertFalse(state.isEditMode)
        assertNull(state.transactionId)
        assertEquals("", state.amount)
        assertEquals(TransactionType.EXPENSE, state.type)
        assertEquals(TransactionCategory.FOOD, state.category)
    }

    @Test
    fun testInitializationWithIdLoadsTransactionForEdit() = runTest {
        viewModel = createViewModel(SavedStateHandle(mapOf("transactionId" to "123")))

        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.isEditMode)
        assertEquals("123", state.transactionId)
        assertEquals("900", state.amount)
        assertEquals(TransactionType.EXPENSE, state.type)
        assertEquals(TransactionCategory.GROCERIES, state.category)
        assertEquals("Week food shopping", state.description)
    }

    @Test
    fun testInitializationWithIdLoadsTransactionForEditSanitizesAndCaps() = runTest {
        viewModel = createViewModel(SavedStateHandle(mapOf("transactionId" to "456")))

        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.isEditMode)
        assertEquals("456", state.transactionId)
        assertEquals("999999", state.amount)
    }

    @Test
    fun testOnAmountChangeAcceptsOnlyNumbers() {
        viewModel = createViewModel()

        // Valid amount changes
        viewModel.onAmountChange("12.50")
        assertEquals("12.50", viewModel.state.value.amount)

        // Invalid amount changes should be ignored
        viewModel.onAmountChange("invalid_amount")
        assertEquals("12.50", viewModel.state.value.amount)
    }

    @Test
    fun testOnAmountChangeRejectsMoreThanTwoDecimalPlaces() {
        viewModel = createViewModel()

        viewModel.onAmountChange("12.50")
        assertEquals("12.50", viewModel.state.value.amount)

        // More than 2 decimal places should be ignored
        viewModel.onAmountChange("12.505")
        assertEquals("12.50", viewModel.state.value.amount)
    }

    @Test
    fun testOnAmountChangeRejectsExtremelyLargeNumber() {
        viewModel = createViewModel()

        viewModel.onAmountChange("999999.00")
        assertEquals("999999.00", viewModel.state.value.amount)

        // Extremely large number (greater than 999999.00) should be ignored
        viewModel.onAmountChange("999999.01")
        assertEquals("999999.00", viewModel.state.value.amount)
    }

    @Test
    fun testInitializationSanitizesPrefillAmountWithCurrencySymbol() {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "title" to "Test Product",
                "amount" to "$123.45",
                "category" to "SHOPPING",
                "description" to "Barcode: 12345"
            )
        )

        viewModel = createViewModel(savedStateHandle)

        assertEquals("123.45", viewModel.state.value.amount)
    }

    @Test
    fun testInitializationSanitizesPrefillAmountWithCommas() {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "title" to "Test Product",
                "amount" to "1,234.56",
                "category" to "SHOPPING",
                "description" to "Barcode: 12345"
            )
        )

        viewModel = createViewModel(savedStateHandle)

        assertEquals("1234.56", viewModel.state.value.amount)
    }

    @Test
    fun testInitializationSanitizesPrefillAmountWithAlphabets() {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "title" to "Test Product",
                "amount" to "123.45abc",
                "category" to "SHOPPING",
                "description" to "Barcode: 12345"
            )
        )

        viewModel = createViewModel(savedStateHandle)

        assertEquals("123.45", viewModel.state.value.amount)
    }

    @Test
    fun testInitializationCapsExtremelyLargePrefillAmount() {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "title" to "Test Product",
                "amount" to "9999999.99",
                "category" to "SHOPPING",
                "description" to "Barcode: 12345"
            )
        )

        viewModel = createViewModel(savedStateHandle)

        assertEquals("999999.00", viewModel.state.value.amount)
    }

    @Test
    fun testSaveTransactionWithValidDataCallsUseCase() = runTest {
        viewModel = createViewModel()

        viewModel.onAmountChange("150")
        viewModel.onTypeChange(TransactionType.INCOME)
        viewModel.onCategoryChange(TransactionCategory.SALARY)
        viewModel.onDescriptionChange("Freelance stipend")

        viewModel.saveTransaction()
        advanceUntilIdle()

        verify(addTransactionUseCase).invoke(any())
        assertTrue(viewModel.state.value.isSuccess)
    }

    @Test
    fun testSaveTransactionWithInvalidDataDisplaysErrorMessage() = runTest {
        viewModel = createViewModel()

        // No amount set
        viewModel.saveTransaction()
        advanceUntilIdle()

        assertEquals("Please enter a valid amount", viewModel.state.value.errorMessage)
    }

    @Test
    fun testInitializationWithPrefillData() {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "title" to "Test Product",
                "amount" to "123.45",
                "category" to "SHOPPING",
                "description" to "Barcode: 12345"
            )
        )

        viewModel = createViewModel(savedStateHandle)

        val state = viewModel.state.value
        assertFalse(state.isEditMode)
        assertEquals("123.45", state.amount)
        assertEquals(TransactionCategory.SHOPPING, state.category)
        assertEquals("Test Product - Barcode: 12345", state.description)
        assertEquals(TransactionType.EXPENSE, state.type)
    }

    @Test
    fun testSaveTransactionWithPrefillBarcodeAppendsToDescription() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "title" to "Cerave Cream",
                "amount" to "299.0",
                "category" to "SHOPPING",
                "description" to "Barcode: 7777777777777"
            )
        )

        viewModel = createViewModel(savedStateHandle)

        // User changes description (clearing prefilled barcode details)
        viewModel.onDescriptionChange("Cerave Cream")

        viewModel.saveTransaction()
        advanceUntilIdle()

        val captor = org.mockito.kotlin.argumentCaptor<Transaction>()
        verify(addTransactionUseCase).invoke(captor.capture())

        val savedTx = captor.firstValue
        assertEquals("Cerave Cream", savedTx.title)
        assertEquals("Cerave Cream", savedTx.description)
    }

    @Test
    fun testOnTypeChangeRemapsCategoryIfUnsupported() {
        viewModel = createViewModel()

        // Default type is EXPENSE, category is OTHERS (supported by EXPENSE).
        // Set category to FOOD (expense-only)
        viewModel.onCategoryChange(TransactionCategory.FOOD)
        assertEquals(TransactionCategory.FOOD, viewModel.state.value.category)

        // Change type to INCOME. FOOD is not supported by INCOME, so it should remap to SALARY.
        viewModel.onTypeChange(TransactionType.INCOME)
        assertEquals(TransactionType.INCOME, viewModel.state.value.type)
        assertEquals(TransactionCategory.SALARY, viewModel.state.value.category)

        // Change category to GIFTS (supported by both INCOME and EXPENSE)
        viewModel.onCategoryChange(TransactionCategory.GIFTS)
        assertEquals(TransactionCategory.GIFTS, viewModel.state.value.category)

        // Change type to EXPENSE. GIFTS is supported by EXPENSE, so it should remain GIFTS.
        viewModel.onTypeChange(TransactionType.EXPENSE)
        assertEquals(TransactionType.EXPENSE, viewModel.state.value.type)
        assertEquals(TransactionCategory.GIFTS, viewModel.state.value.category)
    }

    @Test
    fun testSaveEditedTransactionCallsUpdateUseCase() = runTest {
        viewModel = createViewModel(SavedStateHandle(mapOf("transactionId" to "123")))
        advanceUntilIdle()

        viewModel.onAmountChange("950")
        viewModel.saveTransaction()
        advanceUntilIdle()

        verify(updateTransactionUseCase).invoke(any())
        verify(addTransactionUseCase, never()).invoke(any())
        assertTrue(viewModel.state.value.isSuccess)
    }
}
