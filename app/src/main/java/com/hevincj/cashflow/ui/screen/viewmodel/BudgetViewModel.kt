package com.hevincj.cashflow.ui.screen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hevincj.cashflow.domain.models.Budget
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.repository.TransactionRepository
import com.hevincj.cashflow.domain.usecase.DeleteBudgetUseCase
import com.hevincj.cashflow.domain.usecase.GetBudgetsWithSpendingUseCase
import com.hevincj.cashflow.domain.usecase.SetBudgetUseCase
import com.hevincj.cashflow.ui.screen.state.BudgetUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList

data class BudgetAlert(
    val category: TransactionCategory,
    val percent: Int, // 80 or 100
    val limit: Double,
    val spent: Double
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val setBudgetUseCase: SetBudgetUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    private val getBudgetsWithSpendingUseCase: GetBudgetsWithSpendingUseCase,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    private val _state = MutableStateFlow(BudgetUiState(isLoading = true))
    val state: StateFlow<BudgetUiState> = _state.asStateFlow()

    private val _alerts = MutableSharedFlow<BudgetAlert>(extraBufferCapacity = 64)
    val alerts: SharedFlow<BudgetAlert> = _alerts.asSharedFlow()

    private val firedAlerts = mutableSetOf<String>()

    init {
        observeBudgetsAndSelectedMonth()
    }

    fun selectMonth(yearMonth: YearMonth) {
        _selectedMonth.value = yearMonth
    }

    fun setBudget(category: TransactionCategory, limit: Double) {
        viewModelScope.launch {
            val month = _selectedMonth.value
            val budget = Budget(
                category = category,
                monthlyLimit = limit,
                spent = 0.0,
                month = month.monthValue,
                year = month.year
            )
            setBudgetUseCase(budget)
        }
    }

    fun deleteBudget(category: TransactionCategory) {
        viewModelScope.launch {
            val month = _selectedMonth.value
            deleteBudgetUseCase(category.name, month.monthValue, month.year)
        }
    }

    private fun observeBudgetsAndSelectedMonth() {
        viewModelScope.launch {
            _selectedMonth
                .flatMapLatest { selectedMonth ->
                    _state.value = _state.value.copy(
                        isLoading = true,
                        selectedMonth = selectedMonth
                    )

                    getBudgetsWithSpendingUseCase(selectedMonth.monthValue, selectedMonth.year)
                        .combine(transactionRepository.getAllTransactions()) { budgets, allTransactions ->
                            // Calculate available months based on transactions
                            val months = (allTransactions.map { tx ->
                                val date = Instant.ofEpochMilli(tx.timestamp)
                                    .atZone(ZoneId.systemDefault()).toLocalDate()
                                YearMonth.of(date.year, date.monthValue)
                            } + YearMonth.now()).distinct().sortedDescending()

                            // Check alerts
                            checkAlerts(budgets)

                            BudgetUiState(
                                budgets = budgets.toImmutableList(),
                                selectedMonth = selectedMonth,
                                availableMonths = months.toImmutableList(),
                                isLoading = false
                            )
                        }
                }
                .collect { newState ->
                    _state.value = newState
                }
        }
    }

    private fun checkAlerts(budgets: List<Budget>) {
        budgets.forEach { budget ->
            val key80 = "${budget.year}_${budget.month}_${budget.category.name}_80"
            val key100 = "${budget.year}_${budget.month}_${budget.category.name}_100"

            if (budget.progress >= 1.0f) {
                if (!firedAlerts.contains(key100)) {
                    firedAlerts.add(key100)
                    firedAlerts.add(key80) // skip 80 warning if we hit 100 immediately
                    _alerts.tryEmit(BudgetAlert(budget.category, 100, budget.monthlyLimit, budget.spent))
                }
            } else if (budget.progress >= 0.8f) {
                if (!firedAlerts.contains(key80)) {
                    firedAlerts.add(key80)
                    _alerts.tryEmit(BudgetAlert(budget.category, 80, budget.monthlyLimit, budget.spent))
                }
                // if they refunded / deleted transaction bringing spending under 100%
                firedAlerts.remove(key100)
            } else {
                // reset alert triggers if they delete/refund transactions bringing progress below 80%
                firedAlerts.remove(key80)
                firedAlerts.remove(key100)
            }
        }
    }
}
