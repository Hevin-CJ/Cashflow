package com.hevincj.cashflow.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hevincj.cashflow.ui.screen.HomeScreen
import com.hevincj.cashflow.ui.theme.*

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.hilt.navigation.compose.hiltViewModel
import com.hevincj.cashflow.ui.screen.viewmodel.StatsViewModel
import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.models.TransactionStats
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.ui.screen.state.StatsUiState
import com.hevincj.cashflow.utils.DateTimeUtils
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

// Cached at file scope — DateTimeFormatter.ofPattern compiles the pattern string
// on every call. These are safe to share across recompositions (immutable).
private val MONTH_LABEL_FORMATTER = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val DATE_RANGE_START_FORMATTER = java.time.format.DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault())
private val DATE_RANGE_END_FORMATTER = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())


@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun StatisticsScreen(
    innerPadding: PaddingValues,
    viewModel: StatsViewModel = hiltViewModel()
) {

    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentTab by remember { mutableStateOf(0) } // 0 = Overview, 1 = Budgets

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = innerPadding.calculateBottomPadding())
    ) {localPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 12.dp)
            ) {
                OverviewTopBar()
            }

            // Tab bar to switch between Overview and Budgets
            TabRow(
                selectedTabIndex = currentTab,
                containerColor = Color.Transparent,
                contentColor = FABBackgroundColor,
                divider = {},
                indicator = { tabPositions ->
                    if (currentTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                            color = FABBackgroundColor
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Tab(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    text = { Text("Overview", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    selectedContentColor = FABBackgroundColor,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    text = { Text("Budgets", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    selectedContentColor = FABBackgroundColor,
                    unselectedContentColor = TextSecondary
                )
            }

            if (currentTab == 0) {
                StatisticsScreenContent(
                    uiState = uiState,
                    modifier = Modifier.weight(1f),
                    onMonthSelected = viewModel::selectMonth
                )
            } else {
                BudgetsScreen(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}

@Composable
fun StatisticsScreenContent(
    uiState: StatsUiState,
    modifier: Modifier = Modifier,
    onMonthSelected: (java.time.YearMonth) -> Unit = {}
) {
    val stats = uiState.stats
    var selectedTab by remember { mutableStateOf(TransactionType.EXPENSE) }

    val filteredTransactions = remember(stats?.recentTransactions?.size, stats?.recentTransactions?.firstOrNull()?.id, selectedTab) {
        stats?.recentTransactions?.filter {
            it.type == selectedTab
        } ?: emptyList()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            bottom = 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { 
            IncomeExpenseSummaryRow(
                income = stats?.totalIncome ?: 0.0,
                expense = stats?.totalExpenses ?: 0.0
            ) 
        }

        item { 
            ChartSection(
                selectedMonth = uiState.selectedMonth,
                availableMonths = uiState.availableMonths,
                onMonthSelected = onMonthSelected,
                incomeData = stats?.weeklyIncome ?: emptyList(),
                expenseData = stats?.weeklyExpenses ?: emptyList()
            ) 
        }

        item { 
            IncomeExpenseTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            ) 
        }

        items(items = filteredTransactions, key = { it.id }) { transaction ->
            CustomStatTransactionItem(
                icon = transaction.icon,
                iconColor = Color(0xFF212121),
                iconBgColor = transaction.iconBgColor,
                category = transaction.title,
                date = DateTimeUtils.formatTimestamp(transaction.timestamp),
                amount = (if (transaction.amount > 0) "+" else "-") + "$" + kotlin.math.abs(transaction.amount).toInt()
            )
        }
    }
}



@Composable
private fun OverviewTopBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = GridIconContainerColor)
        ) {
            Icon(
                imageVector = Icons.Rounded.GridView,
                contentDescription = null,
                modifier = Modifier.padding(12.dp).size(24.dp),
                tint = TextPrimary
            )
        }
        Text(
            text = "Overview",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        // Placeholder to keep the text perfectly centered
        Box(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun IncomeExpenseSummaryRow(income: Double, expense: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SummaryCard(
            modifier = Modifier.weight(1f).height(100.dp),
            backgroundColor = IncomePurpleColor.copy(alpha = 0.1f),
            title = "Total Income",
            value = "$${income.toInt()}",
            icon = Icons.Rounded.ArrowDownward,
            iconColor = IncomePurpleColor,
            textColor = TextPrimary
        )
        SummaryCard(
            modifier = Modifier.weight(1f).height(100.dp),
            backgroundColor = ExpenseOrangeColor.copy(alpha = 0.1f),
            title = "Total Expenses",
            value = "$${expense.toInt()}",
            icon = Icons.Rounded.ArrowUpward,
            iconColor = ExpenseOrangeColor,
            textColor = TextPrimary
        )
    }
}

@Composable
private fun ChartSection(
    selectedMonth: java.time.YearMonth,
    availableMonths: List<java.time.YearMonth>,
    onMonthSelected: (java.time.YearMonth) -> Unit,
    incomeData: List<Float>,
    expenseData: List<Float>
) {
    val dateRange = remember(selectedMonth) {
        val startLocalDate = selectedMonth.atDay(1)
        val endLocalDate = selectedMonth.atEndOfMonth()
        // Use file-level cached formatters — no per-recompose pattern compilation.
        "${startLocalDate.format(DATE_RANGE_START_FORMATTER)} - ${endLocalDate.format(DATE_RANGE_END_FORMATTER)}"
    }

    val monthLabel = remember(selectedMonth) {
        selectedMonth.format(MONTH_LABEL_FORMATTER)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Statistics",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = dateRange,
                    color = ChartLabelColor,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }
            var expanded by remember { mutableStateOf(false) }
            Box {
                Card(
                    modifier = Modifier.clickable { expanded = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = TabUnselectedColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = monthLabel,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = TextPrimary
                        )
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.heightIn(max = 240.dp)
                ) {
                    availableMonths.forEach { month ->
                        // Use the file-level cached formatter — no per-item pattern compilation.
                        val label = month.format(MONTH_LABEL_FORMATTER)
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onMonthSelected(month)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (incomeData.isNotEmpty() && expenseData.isNotEmpty()) {
            GroupedBarChart(
                incomeData = incomeData,
                expenseData = expenseData,
                // Pass selectedMonth so the chart only re-animates when the user
                // picks a different month, not on every background DB update.
                selectedMonth = selectedMonth,
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 50.dp), // Align with chart
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Week 1", "Week 2", "Week 3", "Week 4").forEach { label ->
                    Text(
                        text = label,
                        color = ChartLabelColor,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomeExpenseTabs(
    selectedTab: TransactionType,
    onTabSelected: (TransactionType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isIncome = selectedTab == TransactionType.INCOME
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onTabSelected(TransactionType.INCOME) },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isIncome) IncomePurpleColor else TabUnselectedColor
            )
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Income",
                    color = if (isIncome) Color.White else TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
        
        val isExpense = selectedTab == TransactionType.EXPENSE
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onTabSelected(TransactionType.EXPENSE) },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isExpense) ExpenseOrangeColor else TabUnselectedColor
            )
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Expenses",
                    color = if (isExpense) Color.White else TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}

// --- Reusable Utilities ---

// OPTIMIZATION: Added `modifier` parameter so it can be dynamically sized
@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    textColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, color = textColor, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape,
                    color = iconColor.copy(alpha = 0.2f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(4.dp).size(16.dp),
                        tint = iconColor
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = value, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GroupedBarChart(
    incomeData: List<Float>,
    expenseData: List<Float>,
    selectedMonth: java.time.YearMonth,
    modifier: Modifier = Modifier
) {
    val barWidth = 12.dp
    val barGap = 4.dp
    val groupedBarGap = 16.dp

    val maxYValue = remember(incomeData, expenseData) {
        val rawMax = (incomeData + expenseData).maxOrNull() ?: 0f
        when {
            rawMax <= 0f -> 1000f
            rawMax <= 100f -> 100f
            rawMax <= 500f -> 500f
            rawMax <= 1000f -> 1000f
            rawMax <= 2000f -> 2000f
            rawMax <= 5000f -> 5000f
            rawMax <= 10000f -> 10000f
            rawMax <= 20000f -> 20000f
            else -> (kotlin.math.ceil(rawMax / 10000f) * 10000f)
        }
    }

    val labels = remember(maxYValue) {
        listOf(
            formatYLabel(maxYValue),
            formatYLabel(maxYValue * 0.75f),
            formatYLabel(maxYValue * 0.5f),
            formatYLabel(maxYValue * 0.25f),
            "$0"
        )
    }

    val animatedProgress = remember { Animatable(0f) }
    // Key on selectedMonth — not on incomeData/expenseData list objects.
    // List<Float> produces a new object on every ViewModel emission, causing the
    // chart to snap to 0 and re-animate on every background DB sync.
    // selectedMonth only changes when the user explicitly picks a month.
    LaunchedEffect(selectedMonth) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Column(
            modifier = Modifier.width(45.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            labels.forEach { label ->
                Text(text = label, color = ChartLabelColor, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        val gridColor = ChartGridLineColor
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridLineCount = 5
                val gridLineSpacing = size.height / (gridLineCount - 1)
                for (i in 0 until gridLineCount) {
                    val y = i * gridLineSpacing
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f),
                        strokeWidth = 1f
                    )
                }

                val numGroups = minOf(incomeData.size, expenseData.size)
                if (numGroups > 0) {
                    val barWidthPx = 12.dp.toPx()
                    val barGapPx = 4.dp.toPx()
                    val cornerRadiusPx = 6.dp.toPx()
                    val groupWidthPx = 2 * barWidthPx + barGapPx

                    for (i in 0 until numGroups) {
                        val centerX = (i + 0.5f) * (size.width / numGroups)
                        val groupStartX = centerX - groupWidthPx / 2
                        
                        val incomeStartX = groupStartX
                        val expenseStartX = groupStartX + barWidthPx + barGapPx

                        // Calculate animated heights in pixels
                        val animatedIncomeHeight = (incomeData[i] / maxYValue) * size.height * animatedProgress.value
                        val animatedExpenseHeight = (expenseData[i] / maxYValue) * size.height * animatedProgress.value

                        // Draw Income Bar (Purple)
                        if (animatedIncomeHeight > 0f) {
                            drawRoundRect(
                                color = IncomePurpleColor,
                                topLeft = Offset(incomeStartX, size.height - animatedIncomeHeight),
                                size = Size(barWidthPx, animatedIncomeHeight),
                                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                            )
                            if (animatedIncomeHeight > cornerRadiusPx) {
                                drawRect(
                                    color = IncomePurpleColor,
                                    topLeft = Offset(incomeStartX, size.height - cornerRadiusPx),
                                    size = Size(barWidthPx, cornerRadiusPx)
                                )
                            }
                        }

                        // Draw Expense Bar (Orange)
                        if (animatedExpenseHeight > 0f) {
                            drawRoundRect(
                                color = ExpenseOrangeColor,
                                topLeft = Offset(expenseStartX, size.height - animatedExpenseHeight),
                                size = Size(barWidthPx, animatedExpenseHeight),
                                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                            )
                            if (animatedExpenseHeight > cornerRadiusPx) {
                                drawRect(
                                    color = ExpenseOrangeColor,
                                    topLeft = Offset(expenseStartX, size.height - cornerRadiusPx),
                                    size = Size(barWidthPx, cornerRadiusPx)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatYLabel(value: Float): String {
    return when {
        value >= 1000f -> {
            val kValue = value / 1000f
            if (kValue % 1f == 0f) {
                "$${kValue.toInt()}k"
            } else {
                "$${String.format(Locale.getDefault(), "%.1f", kValue)}k"
            }
        }
        else -> "$${value.toInt()}"
    }
}

fun calculateBarHeight(value: Float, maxValue: Float, chartHeightDp: Dp): Dp {
    return (value / maxValue * chartHeightDp.value).dp
}

// Customized Item just for the stats screen to handle the special icon backgrounds
@Composable
private fun CustomStatTransactionItem(
    icon: ImageVector,
    iconColor: Color,
    iconBgColor: Color,
    category: String,
    date: String,
    amount: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = iconBgColor)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(14.dp).size(28.dp),
                tint = iconColor
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category,
                color = TextPrimaryColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = date, color = TextSecondaryColor, fontSize = 14.sp)
        }
        Text(
            text = amount,
            color = if (amount.startsWith("-")) NegativeAmountColor else PositiveGreen,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


@Preview(showBackground = true)
@Composable
fun StatisticsDashboardPreview() {
    val mockStats = TransactionStats(
        totalIncome = 3200.0,
        totalExpenses = 1450.0,
        weeklyIncome = listOf(1000f, 2500f, 1500f, 3000f),
        weeklyExpenses = listOf(800f, 1200f, 1100f, 1450f),
        recentTransactions = listOf(
            Transaction("1", "Money Transfer", System.currentTimeMillis(), -450.0, Icons.Rounded.Person, Color(0xFFF3F4F6), TransactionType.EXPENSE, TransactionCategory.OTHERS, "Money Transfer", true),
            Transaction("2", "Paypal", System.currentTimeMillis() - 2 * 3600 * 1000L, 1200.0, Icons.Rounded.Payment, Color(0xFFF3F4F6), TransactionType.INCOME, TransactionCategory.SALARY, "Paypal payment", true),
            Transaction("3", "Uber", System.currentTimeMillis() - 4 * 3600 * 1000L, -150.0, Icons.Rounded.DirectionsCar, Color(0xFFF3F4F6), TransactionType.EXPENSE, TransactionCategory.TRANSPORT, "Uber ride", true)
        )
    )

    MaterialTheme {
        StatisticsScreenContent(
            uiState = StatsUiState(stats = mockStats)
        )
    }
}