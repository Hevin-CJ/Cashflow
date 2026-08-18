package com.hevincj.cashflow.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.roundToInt
import com.hevincj.cashflow.ui.screen.state.MonthlyNetSavings
import java.time.YearMonth

import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.platform.LocalDensity

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
fun ShimmerStatisticsScreenContent(shimmerTranslateProvider: () -> Float, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Shimmer for IncomeExpenseSummaryRow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(16.dp))
                .shimmerEffect(shimmerTranslateProvider)
        )

        // Shimmer for NetSavingsLineChart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(24.dp))
                .shimmerEffect(shimmerTranslateProvider)
        )

        // Shimmer for ChartSection (Weekly Bar Chart)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(24.dp))
                .shimmerEffect(shimmerTranslateProvider)
        )
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
    var selectedCategory by remember { mutableStateOf<TransactionCategory?>(null) }

    LaunchedEffect(selectedTab, uiState.selectedMonth) {
        selectedCategory = null
    }

    val filteredTransactions = remember(stats?.recentTransactions, selectedTab) {
        stats?.recentTransactions?.filter {
            it.type == selectedTab
        }?.toImmutableList() ?: persistentListOf()
    }

    val displayedTransactions = remember(filteredTransactions, selectedCategory) {
        if (selectedCategory != null) {
            filteredTransactions.filter { it.category == selectedCategory }.toImmutableList()
        } else {
            filteredTransactions
        }
    }

    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslateAnim = shimmerTransition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val shimmerTranslateProvider = remember(shimmerTranslateAnim) { { shimmerTranslateAnim.value } }

    Crossfade(
        targetState = uiState.isLoading,
        modifier = modifier.fillMaxWidth(),
        label = "statsContentTransition"
    ) { isLoading ->
        if (isLoading) {
            ShimmerStatisticsScreenContent(shimmerTranslateProvider = shimmerTranslateProvider)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item(contentType = "summary") { 
                    IncomeExpenseSummaryRow(
                        income = stats?.totalIncome ?: 0.0,
                        expense = stats?.totalExpenses ?: 0.0
                    ) 
                }

                item(contentType = "line_chart") {
                    NetSavingsLineChart(
                        netSavingsTrend = uiState.netSavingsTrend,
                        selectedMonth = uiState.selectedMonth,
                        onMonthSelected = onMonthSelected
                    )
                }

                item(contentType = "bar_chart") { 
                    ChartSection(
                        selectedMonth = uiState.selectedMonth,
                        availableMonths = uiState.availableMonths,
                        onMonthSelected = onMonthSelected,
                        incomeData = stats?.weeklyIncome ?: persistentListOf(),
                        expenseData = stats?.weeklyExpenses ?: persistentListOf()
                    ) 
                }

                item(contentType = "tabs") { 
                    IncomeExpenseTabs(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    ) 
                }

                item(contentType = "donut_chart") {
                    CategoryDonutChart(
                        filteredTransactions = filteredTransactions,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        selectedTab = selectedTab
                    )
                }

                items(
                    items = displayedTransactions,
                    key = { it.id },
                    contentType = { "transaction" }
                ) { transaction ->
                    CustomStatTransactionItem(
                        icon = transaction.category.icon,
                        iconColor = Color(0xFF212121),
                        iconBgColor = transaction.category.iconBgColor,
                        category = transaction.title,
                        date = transaction.formattedDate,
                        amount = (if (transaction.amount > 0) "+" else "-") + "$" + kotlin.math.abs(transaction.amount).toInt()
                    )
                }
            }
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
    availableMonths: ImmutableList<java.time.YearMonth>,
    onMonthSelected: (java.time.YearMonth) -> Unit,
    incomeData: ImmutableList<Float>,
    expenseData: ImmutableList<Float>
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

        Spacer(modifier = Modifier.height(16.dp))

        // Chart Legend (Income / Expense)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(IncomePurpleColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Income",
                color = ChartLabelColor,
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(ExpenseOrangeColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Expense",
                color = ChartLabelColor,
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(55.dp)) // Match Y-axis labels (45dp) + spacer (10dp)
                Row(
                    modifier = Modifier.weight(1f)
                ) {
                    listOf("Week 1", "Week 2", "Week 3", "Week 4").forEach { label ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
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
    incomeData: ImmutableList<Float>,
    expenseData: ImmutableList<Float>,
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
        val gridPathEffect = remember { PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f) }
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer()
            ) {
                val gridLineCount = 5
                val gridLineSpacing = size.height / (gridLineCount - 1)
                for (i in 0 until gridLineCount) {
                    val y = i * gridLineSpacing
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        pathEffect = gridPathEffect,
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
                        // Calculate animated heights in pixels
                        val rawIncomeHeight = (incomeData[i] / maxYValue) * size.height * animatedProgress.value
                        val rawExpenseHeight = (expenseData[i] / maxYValue) * size.height * animatedProgress.value

                        val animatedIncomeHeight = if (incomeData[i] > 0f) maxOf(rawIncomeHeight, cornerRadiusPx) else 0f
                        val animatedExpenseHeight = if (expenseData[i] > 0f) maxOf(rawExpenseHeight, cornerRadiusPx) else 0f

                        // Draw Income Bar (Purple)
                        if (animatedIncomeHeight > 0f) {
                            val path = Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        left = incomeStartX,
                                        top = size.height - animatedIncomeHeight,
                                        right = incomeStartX + barWidthPx,
                                        bottom = size.height,
                                        topLeftCornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                                        topRightCornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                                    )
                                )
                            }
                            drawPath(path, color = IncomePurpleColor)
                        }

                        // Draw Expense Bar (Orange)
                        if (animatedExpenseHeight > 0f) {
                            val path = Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        left = expenseStartX,
                                        top = size.height - animatedExpenseHeight,
                                        right = expenseStartX + barWidthPx,
                                        bottom = size.height,
                                        topLeftCornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                                        topRightCornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                                    )
                                )
                            }
                            drawPath(path, color = ExpenseOrangeColor)
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = iconBgColor)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(9.dp).size(24.dp),
                tint = iconColor
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category,
                color = TextPrimaryColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = date, color = TextSecondaryColor, fontSize = 12.sp)
        }
        Text(
            text = amount,
            color = if (amount.startsWith("-")) NegativeAmountColor else PositiveGreen,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
fun CategoryDonutChart(
    filteredTransactions: ImmutableList<Transaction>,
    selectedCategory: TransactionCategory?,
    onCategorySelected: (TransactionCategory?) -> Unit,
    selectedTab: TransactionType,
    modifier: Modifier = Modifier
) {
    val totalAmount = remember(filteredTransactions) {
        filteredTransactions.sumOf { kotlin.math.abs(it.amount) }
    }

    val categorySums = remember(filteredTransactions) {
        filteredTransactions.groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> kotlin.math.abs(tx.amount) } }
            .toList()
            .sortedByDescending { it.second }
    }

    val unselectedColor = TabUnselectedColor
    val textPrimary = TextPrimary
    val textSecondary = TextSecondary
    val accentColor = if (selectedTab == TransactionType.EXPENSE) ExpenseOrangeColor else IncomePurpleColor

    if (totalAmount == 0.0) {
        Card(
            modifier = modifier.fillMaxWidth().height(160.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = unselectedColor)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No transactions for this period",
                    color = textSecondary,
                    fontSize = 14.sp
                )
            }
        }
        return
    }

    val chartColors = if (selectedTab == TransactionType.EXPENSE) {
        listOf(
            Color(0xFFFF5722), // Orange (primary expense)
            Color(0xFFFF4081), // Pink
            Color(0xFFFF9100), // Amber
            Color(0xFFFFD600), // Yellow
            Color(0xFFE53935), // Red
            Color(0xFF8121FD), // Purple
            Color(0xFF651FFF), // Deep Purple
            Color(0xFF00B0FF), // Blue
            Color(0xFF00E676), // Green
            Color(0xFF1DE9B6)  // Teal
        )
    } else {
        listOf(
            Color(0xFF8121FD), // Purple (primary income)
            Color(0xFF00B0FF), // Blue
            Color(0xFF00E676), // Green
            Color(0xFF1DE9B6), // Teal
            Color(0xFF3D5AFE), // Indigo
            Color(0xFF651FFF), // Deep Purple
            Color(0xFFFFD600), // Yellow
            Color(0xFFFF9100), // Amber
            Color(0xFFFF5722), // Orange
            Color(0xFFFF4081)  // Pink
        )
    }

    val categoryColors = remember(categorySums, selectedTab) {
        categorySums.mapIndexed { index, pair ->
            pair.first to chartColors[index % chartColors.size]
        }.toMap()
    }

    val categoryPercentages = remember(categorySums, totalAmount) {
        if (totalAmount <= 0.0 || categorySums.isEmpty()) {
            emptyMap<TransactionCategory, Double>()
        } else {
            val scale = 10
            val scaledTotal = 100 * scale // 1000 for 1 decimal place precision
            val initialFloors = categorySums.map { (category, amount) ->
                val rawPct = (amount / totalAmount) * 100.0
                val scaledVal = rawPct * scale
                val floorVal = kotlin.math.floor(scaledVal).toInt()
                val remainder = scaledVal - floorVal
                Triple(category, floorVal, remainder)
            }
            val sumFloors = initialFloors.sumOf { it.second }
            val diff = scaledTotal - sumFloors
            
            // Sort by remainder descending
            val sortedByRemainder = initialFloors.sortedByDescending { it.third }
            val distributedMap = initialFloors.associate { it.first to it.second }.toMutableMap()
            for (i in 0 until diff) {
                if (i < sortedByRemainder.size) {
                    val cat = sortedByRemainder[i].first
                    distributedMap[cat] = (distributedMap[cat] ?: 0) + 1
                }
            }
            distributedMap.mapValues { it.value.toDouble() / scale }
        }
    }

    val density = LocalDensity.current
    val strokeSelected = remember(density) {
        Stroke(width = with(density) { 28.dp.toPx() }, cap = StrokeCap.Butt)
    }
    val strokeUnselected = remember(density) {
        Stroke(width = with(density) { 20.dp.toPx() }, cap = StrokeCap.Butt)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = unselectedColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Category Breakdown",
                color = textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer()
                            .pointerInput(categorySums, totalAmount) {
                                detectTapGestures { offset ->
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    val dx = offset.x - centerX
                                    val dy = offset.y - centerY
                                    val distance = sqrt(dx * dx + dy * dy)

                                    val outerRadius = size.width / 2f
                                    val innerRadius = outerRadius - 24.dp.toPx()

                                    if (distance in innerRadius..outerRadius) {
                                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        angle += 90f
                                        if (angle < 0) angle += 360f

                                        var currentStartAngle = 0f
                                        var foundCategory: TransactionCategory? = null
                                        for ((category, amount) in categorySums) {
                                            val sweep = (amount.toFloat() / totalAmount.toFloat()) * 360f
                                            if (angle >= currentStartAngle && angle <= currentStartAngle + sweep) {
                                                foundCategory = category
                                                break
                                            }
                                            currentStartAngle += sweep
                                        }
                                        onCategorySelected(if (selectedCategory == foundCategory) null else foundCategory)
                                    } else {
                                        onCategorySelected(null)
                                    }
                                }
                            }
                    ) {
                        var currentStartAngle = -90f
                        categorySums.forEach { (category, amount) ->
                            val sweep = (amount.toFloat() / totalAmount.toFloat()) * 360f
                            val isSelected = selectedCategory == category
                            val isAnySelected = selectedCategory != null
                            val color = categoryColors[category] ?: Color.Gray
                            val alpha = if (isSelected) 1f else if (isAnySelected) 0.3f else 1f

                            drawArc(
                                color = color.copy(alpha = alpha),
                                startAngle = currentStartAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = if (isSelected) strokeSelected else strokeUnselected
                            )
                            currentStartAngle += sweep
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(28.dp)
                    ) {
                        val label = selectedCategory?.displayName ?: "Total"
                        val amount = if (selectedCategory != null) {
                            categorySums.find { it.first == selectedCategory }?.second ?: 0.0
                        } else {
                            totalAmount
                        }
                        val percentage = if (selectedCategory != null) {
                            categoryPercentages[selectedCategory] ?: 0.0
                        } else {
                            100.0
                        }
                        val percentageStr = String.format(java.util.Locale.getDefault(), "%.1f", percentage)

                        Text(
                            text = label,
                            color = textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$${amount.roundToInt()}",
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "$percentageStr%",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categorySums.forEach { (category, amount) ->
                    val isSelected = selectedCategory == category
                    val isAnySelected = selectedCategory != null
                    val color = categoryColors[category] ?: Color.Gray
                    val percentage = categoryPercentages[category] ?: 0.0
                    val percentageStr = String.format(java.util.Locale.getDefault(), "%.1f", percentage)
                    val alpha = if (isSelected) 1f else if (isAnySelected) 0.5f else 1f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategorySelected(if (isSelected) null else category) }
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(color.copy(alpha = alpha), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = category.displayName,
                            color = if (isSelected) accentColor else textPrimary.copy(alpha = alpha),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$${amount.roundToInt()} ($percentageStr%)",
                            color = textSecondary.copy(alpha = alpha),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NetSavingsLineChart(
    netSavingsTrend: ImmutableList<MonthlyNetSavings>,
    selectedMonth: YearMonth,
    onMonthSelected: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayedTrend = remember(netSavingsTrend) {
        netSavingsTrend.takeLast(6)
    }

    val unselectedColor = TabUnselectedColor
    val textPrimary = TextPrimary
    val textSecondary = TextSecondary
    val chartLabelColor = ChartLabelColor
    val gridColor = ChartGridLineColor
    val accentColor = FABBackgroundColor

    if (displayedTrend.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth().height(160.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = unselectedColor)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Insufficient data for savings trend",
                    color = textSecondary,
                    fontSize = 14.sp
                )
            }
        }
        return
    }

    val maxVal = remember(displayedTrend) {
        val rawMax = displayedTrend.map { it.amount }.maxOrNull() ?: 100.0
        if (rawMax == 0.0) 100.0 else rawMax
    }
    val minVal = remember(displayedTrend) {
        val rawMin = displayedTrend.map { it.amount }.minOrNull() ?: -100.0
        if (rawMin == 0.0) -100.0 else rawMin
    }

    val range = maxVal - minVal
    val adjustedMax = if (range == 0.0) maxVal + 100.0 else maxVal + range * 0.15
    val adjustedMin = if (range == 0.0) minVal - 100.0 else minVal - range * 0.15
    val adjustedRange = adjustedMax - adjustedMin

    val density = LocalDensity.current
    val gridPathEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) }
    val interactivePathEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) }
    val lineStroke = remember(density) {
        Stroke(width = with(density) { 3.dp.toPx() }, cap = StrokeCap.Round)
    }
    val linePath = remember { Path() }
    val fillPath = remember { Path() }
    val gradientBrush = remember(accentColor) {
        Brush.verticalGradient(
            colors = listOf(accentColor.copy(alpha = 0.25f), Color.Transparent)
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = unselectedColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Net Savings Trend",
                color = textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer()
                ) {
                    val pointsCount = displayedTrend.size
                    if (pointsCount > 0) {
                        val widthStep = if (pointsCount > 1) size.width / (pointsCount - 1) else size.width

                        // 1. Draw horizontal grid lines
                        val gridLineCount = 3
                        val gridLineSpacing = size.height / (gridLineCount - 1)
                        for (i in 0 until gridLineCount) {
                            val y = i * gridLineSpacing
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                pathEffect = gridPathEffect,
                                strokeWidth = 1f
                            )
                        }

                        // 2. Draw curve fill path (gradient area under the line)
                        if (pointsCount > 1) {
                            fillPath.reset()
                            fillPath.moveTo(0f, size.height)
                            
                            val firstY = (size.height - ((displayedTrend[0].amount - adjustedMin) / adjustedRange) * size.height).toFloat()
                            fillPath.lineTo(0f, firstY)
                            
                            for (i in 0 until pointsCount - 1) {
                                val x0 = i * widthStep
                                val y0 = (size.height - ((displayedTrend[i].amount - adjustedMin) / adjustedRange) * size.height).toFloat()
                                val x1 = (i + 1) * widthStep
                                val y1 = (size.height - ((displayedTrend[i + 1].amount - adjustedMin) / adjustedRange) * size.height).toFloat()
                                
                                val cx1 = x0 + (x1 - x0) / 2f
                                val cy1 = y0
                                val cx2 = x0 + (x1 - x0) / 2f
                                val cy2 = y1
                                fillPath.cubicTo(cx1, cy1, cx2, cy2, x1, y1)
                            }
                            fillPath.lineTo((pointsCount - 1) * widthStep, size.height)
                            fillPath.close()

                            drawPath(
                                path = fillPath,
                                brush = gradientBrush
                            )

                            // 3. Draw the smooth curve line
                            linePath.reset()
                            linePath.moveTo(0f, firstY)
                            
                            for (i in 0 until pointsCount - 1) {
                                val x0 = i * widthStep
                                val y0 = (size.height - ((displayedTrend[i].amount - adjustedMin) / adjustedRange) * size.height).toFloat()
                                val x1 = (i + 1) * widthStep
                                val y1 = (size.height - ((displayedTrend[i + 1].amount - adjustedMin) / adjustedRange) * size.height).toFloat()
                                
                                val cx1 = x0 + (x1 - x0) / 2f
                                val cy1 = y0
                                val cx2 = x0 + (x1 - x0) / 2f
                                val cy2 = y1
                                linePath.cubicTo(cx1, cy1, cx2, cy2, x1, y1)
                            }

                            drawPath(
                                path = linePath,
                                color = accentColor,
                                style = lineStroke
                            )
                        }

                        // 4. Draw interactive elements & dots
                        for (index in 0 until pointsCount) {
                            val item = displayedTrend[index]
                            val isSelected = item.month == selectedMonth
                            val px = index * widthStep
                            val py = (size.height - ((item.amount - adjustedMin) / adjustedRange) * size.height).toFloat()

                            if (isSelected) {
                                drawLine(
                                    color = accentColor.copy(alpha = 0.4f),
                                    start = Offset(px, 0f),
                                    end = Offset(px, size.height),
                                    pathEffect = interactivePathEffect,
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            drawCircle(
                                color = if (isSelected) Color.White else accentColor,
                                radius = if (isSelected) 8.dp.toPx() else 5.dp.toPx(),
                                center = Offset(px, py)
                            )
                            if (isSelected) {
                                drawCircle(
                                    color = accentColor,
                                    radius = 5.dp.toPx(),
                                    center = Offset(px, py)
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(displayedTrend) {
                            detectTapGestures { offset ->
                                val pointsCount = displayedTrend.size
                                if (pointsCount > 0) {
                                    val widthStep = if (pointsCount > 1) size.width / (pointsCount - 1) else size.width
                                    var closestIndex = -1
                                    var minDistance = Float.MAX_VALUE
                                    
                                    for (i in 0 until pointsCount) {
                                        val pointX = i * widthStep
                                        val dist = kotlin.math.abs(offset.x - pointX)
                                        if (dist < minDistance) {
                                            minDistance = dist
                                            closestIndex = i
                                        }
                                    }

                                    if (closestIndex != -1 && minDistance < 40.dp.toPx()) {
                                        onMonthSelected(displayedTrend[closestIndex].month)
                                    }
                                }
                            }
                        }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // X-axis labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                displayedTrend.forEach { trendItem ->
                    val isSelected = trendItem.month == selectedMonth
                    val label = trendItem.month.format(
                        java.time.format.DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onMonthSelected(trendItem.month) }
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) accentColor else chartLabelColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$${trendItem.amount.toInt()}",
                            color = if (isSelected) accentColor.copy(alpha = 0.8f) else chartLabelColor.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StatisticsDashboardPreview() {
    val mockStats = TransactionStats(
        totalIncome = 3200.0,
        totalExpenses = 1450.0,
        weeklyIncome = persistentListOf(1000f, 2500f, 1500f, 3000f),
        weeklyExpenses = persistentListOf(800f, 1200f, 1100f, 1450f),
        recentTransactions = persistentListOf(
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