package com.hevincj.cashflow.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hevincj.cashflow.domain.models.Budget
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.ui.screen.state.BudgetUiState
import com.hevincj.cashflow.ui.screen.viewmodel.BudgetViewModel
import com.hevincj.cashflow.ui.theme.*
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

@Composable
fun BudgetsScreen(
    modifier: Modifier = Modifier,
    viewModel: BudgetViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.alerts.collect { alert ->
            val message = when (alert.percent) {
                100 -> "Alert: You've exceeded your ${alert.category.displayName} budget! ($${alert.spent.toInt()}/$${alert.limit.toInt()})"
                else -> "Warning: You've used ${alert.percent}% of your ${alert.category.displayName} budget. ($${alert.spent.toInt()}/$${alert.limit.toInt()})"
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    BudgetsScreenContent(
        uiState = uiState,
        modifier = modifier,
        onMonthSelected = viewModel::selectMonth,
        onDeleteBudget = viewModel::deleteBudget,
        onAddBudgetClick = { showAddDialog = true }
    )

    if (showAddDialog) {
        AddBudgetDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { category, limit ->
                viewModel.setBudget(category, limit)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun BudgetsScreenContent(
    uiState: BudgetUiState,
    modifier: Modifier = Modifier,
    onMonthSelected: (YearMonth) -> Unit = {},
    onDeleteBudget: (TransactionCategory) -> Unit = {},
    onAddBudgetClick: () -> Unit = {}
) {
    val monthLabel = remember(uiState.selectedMonth) {
        uiState.selectedMonth.format(MONTH_LABEL_FORMATTER)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // Month Selector and Add Budget button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                            fontSize = 14.sp
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
                    uiState.availableMonths.forEach { month ->
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

            Button(
                onClick = onAddBudgetClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FABBackgroundColor)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add Budget",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Add Budget", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = FABBackgroundColor)
            }
        } else if (uiState.budgets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Savings,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = TextSecondary
                    )
                    Text(
                        text = "No budgets set for this month",
                        color = TextSecondary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Tap 'Add Budget' to set limits and track category spending.",
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(uiState.budgets, key = { it.category.name }) { budget ->
                    BudgetCard(
                        budget = budget,
                        onDeleteClick = { onDeleteBudget(budget.category) }
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetCard(
    budget: Budget,
    onDeleteClick: () -> Unit
) {
    val progress = budget.progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500),
        label = "budget_progress"
    )

    val progressColor by animateColorAsState(
        targetValue = when {
            budget.isExceeded -> NegativeRed
            budget.isWarning -> Color(0xFFFFB300) // Warning Orange/Yellow
            else -> PositiveGreen
        },
        animationSpec = tween(300),
        label = "budget_progress_color"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(budget.category.iconBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = budget.category.icon,
                            contentDescription = null,
                            tint = Color(0xFF212121),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = budget.category.displayName,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (budget.isExceeded) "Exceeded limit" else if (budget.isWarning) "Nearing limit" else "On track",
                            color = progressColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete Budget",
                        tint = NegativeRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Spending progress values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "$${budget.spent.toInt()} spent",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "of $${budget.monthlyLimit.toInt()}",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = TabUnselectedColor,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun AddBudgetDialog(
    onDismiss: () -> Unit,
    onConfirm: (TransactionCategory, Double) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(TransactionCategory.FOOD) }
    var limitInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    // Retrieve categories supporting expenses
    val categories = remember {
        TransactionCategory.values().filter { it.supportedTypes.contains(TransactionType.EXPENSE) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Set Category Budget",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "SELECT CATEGORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                // Category scrollable selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategory == category
                        val cardBg = if (isSelected) TabUnselectedColor else Color.Transparent
                        val borderMod = if (isSelected) Modifier.border(1.dp, FABBackgroundColor, RoundedCornerShape(12.dp)) else Modifier

                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardBg)
                                .then(borderMod)
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(category.iconBgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = Color(0xFF212121),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = category.displayName,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Text(
                    text = "MONTHLY LIMIT AMOUNT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            limitInput = input
                            errorText = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. 150") },
                    prefix = { Text("$ ", color = FABBackgroundColor, fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FABBackgroundColor,
                        cursorColor = FABBackgroundColor
                    )
                )

                if (errorText.isNotEmpty()) {
                    Text(
                        text = errorText,
                        color = NegativeRed,
                        fontSize = 12.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val limit = limitInput.toDoubleOrNull()
                            if (limit == null || limit <= 0.0) {
                                errorText = "Please enter a valid budget limit"
                            } else {
                                onConfirm(selectedCategory, limit)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FABBackgroundColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Budget")
                    }
                }
            }
        }
    }
}
