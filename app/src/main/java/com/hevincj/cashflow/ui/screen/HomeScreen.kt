    package com.hevincj.cashflow.ui.screen

    import androidx.compose.foundation.background
    import androidx.compose.foundation.border
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.PaddingValues
    import androidx.compose.foundation.layout.Row
    import androidx.compose.foundation.layout.Spacer
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.fillMaxWidth
    import androidx.compose.foundation.layout.height
    import androidx.compose.foundation.layout.offset
    import androidx.compose.foundation.layout.padding
    import androidx.compose.foundation.layout.size
    import androidx.compose.foundation.layout.width
    import androidx.compose.foundation.lazy.LazyColumn
    import androidx.compose.foundation.lazy.items
    import androidx.compose.foundation.lazy.itemsIndexed
    import androidx.compose.foundation.shape.CircleShape
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.rounded.AccountBalance
    import androidx.compose.material.icons.rounded.ArrowDownward
    import androidx.compose.material.icons.rounded.ArrowUpward
    import androidx.compose.material.icons.rounded.DirectionsCar
    import androidx.compose.material.icons.rounded.QrCodeScanner
    import androidx.compose.material.icons.rounded.KeyboardArrowDown
    import androidx.compose.material.icons.rounded.MoreHoriz
    import androidx.compose.material.icons.rounded.Layers
    import androidx.compose.material.icons.rounded.NotificationsNone
    import androidx.compose.material.icons.rounded.Payment
    import androidx.compose.material.icons.rounded.Person
    import androidx.compose.material.icons.rounded.ShoppingBag
    import androidx.compose.material.icons.rounded.Refresh
    import androidx.compose.material.icons.rounded.Warning
    import androidx.compose.material3.DropdownMenu
    import androidx.compose.material3.DropdownMenuItem
    import androidx.compose.material3.MenuDefaults
    import androidx.compose.material3.Icon
    import androidx.compose.material3.IconButton
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.Text
    import androidx.compose.material3.Card
    import androidx.compose.material3.CardDefaults
    import androidx.compose.material3.Button
    import androidx.compose.material3.ButtonDefaults
    import androidx.compose.foundation.layout.heightIn
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.text.style.TextOverflow
    import androidx.compose.runtime.remember
    import androidx.compose.runtime.key
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.draw.drawBehind
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.tooling.preview.Preview
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import com.hevincj.cashflow.domain.models.Transaction
    import com.hevincj.cashflow.ui.theme.NegativeRed
    import com.hevincj.cashflow.ui.theme.PositiveGreen
    import com.hevincj.cashflow.ui.theme.PrimaryGradient
    import com.hevincj.cashflow.ui.theme.TextPrimary
    import com.hevincj.cashflow.ui.theme.TextSecondary
    import com.hevincj.cashflow.ui.theme.CardBackground
    import com.hevincj.cashflow.ui.theme.BackgroundGray
    import com.hevincj.cashflow.ui.theme.LocalDarkTheme
    import kotlin.math.abs
    import com.hevincj.cashflow.utils.DateTimeUtils


    import androidx.lifecycle.compose.collectAsStateWithLifecycle
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.setValue
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.saveable.rememberSaveable
    import androidx.hilt.navigation.compose.hiltViewModel
    import com.hevincj.cashflow.ui.screen.state.BalanceRange
    import com.hevincj.cashflow.ui.screen.state.HomeUiState
    import com.hevincj.cashflow.ui.screen.viewmodel.HomeViewModel
    import com.hevincj.cashflow.domain.models.TransactionType
    import java.text.DecimalFormat


import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import android.widget.Toast
import com.hevincj.cashflow.ui.screen.viewmodel.ScanViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope


    @Composable
    fun HomeScreen(
        innerPadding: PaddingValues,
        rootNavController: NavController = rememberNavController(),
        viewModel: HomeViewModel = hiltViewModel(),
        scanViewModel: ScanViewModel = hiltViewModel()
    ) {
        val uiState by viewModel.state.collectAsStateWithLifecycle()
        val listState = rememberLazyListState()
        var previousSize by rememberSaveable { mutableStateOf(-1) }

        // Auto-scroll to the top ONLY when a brand-new transaction is added.
        // Key on size (an Int) so this effect doesn't re-fire on every DB emission
        // that produces the same list with a new object identity.
        LaunchedEffect(uiState.transactions.size) {
            val currentSize = uiState.transactions.size
            if (currentSize > 0) {
                if (previousSize != -1 && currentSize > previousSize) {
                    listState.animateScrollToItem(0)
                }
                previousSize = currentSize
            }
        }

        // Trigger initial sync to refresh data from the server.
        // Small delay lets the enter animation complete before kicking off network I/O.
        LaunchedEffect(Unit) {
            delay(200)
            viewModel.refreshSync(force = true, limit = 25)
        }

        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        var showUpiQrDialog by remember { mutableStateOf(false) }

        if (showUpiQrDialog) {
            UpiPaymentQrDialog(
                onDismissRequest = { showUpiQrDialog = false },
                viewModel = scanViewModel
            )
        }

        val onRangeSelected = remember(viewModel) { { range: BalanceRange -> viewModel.onBalanceRangeChange(range) } }
        val onDeleteTransaction = remember(viewModel) { { transaction: Transaction -> viewModel.deleteTransaction(transaction) } }
        val onRetrySync = remember(viewModel) { { viewModel.refreshSync() } }
        val onNavigateToAddTransaction = remember(rootNavController) { { transactionId: String? -> rootNavController.navigate("add_transaction?transactionId=$transactionId") } }
        val onNavigateToAllTransactions = remember(rootNavController) { { rootNavController.navigate("all_transactions") } }

        HomeScreenContent(
            innerPadding = innerPadding,
            uiState = uiState,
            onRangeSelected = onRangeSelected,
            onDeleteTransaction = onDeleteTransaction,
            onRetrySync = onRetrySync,
            onNavigateToAddTransaction = onNavigateToAddTransaction,
            onNavigateToAllTransactions = onNavigateToAllTransactions,
            rootNavController = rootNavController,
            listState = listState
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HomeScreenContent(
        innerPadding: PaddingValues,
        uiState: HomeUiState,
        onRangeSelected: (BalanceRange) -> Unit,
        onDeleteTransaction: (Transaction) -> Unit,
        onRetrySync: () -> Unit,
        onNavigateToAddTransaction: (String?) -> Unit,
        onNavigateToAllTransactions: () -> Unit,
        rootNavController: NavController,
        listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState()
    ) {
        var showBatchItemsTransaction by remember { mutableStateOf<Transaction?>(null) }

        if (showBatchItemsTransaction != null) {
            val items = parseBatchDescription(showBatchItemsTransaction?.description)
            Dialog(
                onDismissRequest = { showBatchItemsTransaction = null }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "Batch Scanned Items",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 240.dp)
                        ) {
                            items(items) { (name, code) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (LocalDarkTheme.current) Color(0xFF2C2C2E) else Color(0xFFF3F4F6))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Barcode: $code",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showBatchItemsTransaction = null },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF635BFF))
                        ) {
                            Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Fixed top bar
            Box(modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 12.dp)) {
                TopNavigationBar(onScanClick = { rootNavController.navigate("scan_hub") })
            }

            // Fixed balance card
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                BalanceCard(
                    balance = uiState.totalBalance,
                    income = uiState.totalIncome,
                    expense = uiState.totalExpense,
                    selectedRange = uiState.balanceRange,
                    isLoading = uiState.isLoading,
                    onRangeSelected = onRangeSelected
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Fixed header
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                TransactionHeader(
                    isLoading = uiState.isLoading,
                    onSeeAllClick = onNavigateToAllTransactions
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFFF0F0))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = "Sync Error",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sync Failure",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uiState.error ?: "",
                                fontSize = 12.sp,
                                color = Color(0xFFD32F2F)
                            )
                        }
                        IconButton(
                            onClick = onRetrySync,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Retry Sync",
                                tint = Color(0xFFC62828)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val transactions = uiState.transactions.take(25)

            if (!uiState.isLoading && transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFFF3E8FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Payment,
                                contentDescription = "No Transactions",
                                tint = Color(0xFF8121FD),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No transactions found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your transaction history will appear here once you make your first payment.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    userScrollEnabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        items(25, key = { "shimmer_$it" }) {
                            // Each ShimmerTransactionItem owns its own infinite animation scope;
                            // only shimmer items recompose at 60fps, not the parent tree.
                            ShimmerTransactionItem()
                        }
                    } else {
                        items(
                            items = transactions,
                            key = { it.id }
                        ) { transaction ->
                            val onBatchIconClickLambda = remember(transaction.id) {
                                {
                                    showBatchItemsTransaction = transaction
                                }
                            }
                            SwipeableTransactionItem(
                                transaction = transaction,
                                onDeleteTransaction = onDeleteTransaction,
                                onNavigateToAddTransaction = onNavigateToAddTransaction,
                                onBatchIconClick = onBatchIconClickLambda
                            )
                        }
                    }
                }
            }
        }
    }


    @Composable
    private fun TopNavigationBar(onScanClick: () -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackground)
                ) {
                    Icon(
                        Icons.Rounded.NotificationsNone,
                        contentDescription = "Notifications",
                        tint = TextPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-8).dp, y = 8.dp)
                        .clip(CircleShape)
                        .background(NegativeRed)
                )
            }

            Text(
                text = "Home",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            IconButton(
                onClick = onScanClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackground)
            ) {
                Icon(
                    Icons.Rounded.QrCodeScanner,
                    contentDescription = "Scan Barcode",
                    tint = TextPrimary
                )
            }
        }
    }

    @Composable
    private fun BalanceCard(
        balance: Double,
        income: Double,
        expense: Double,
        selectedRange: BalanceRange,
        isLoading: Boolean,
        onRangeSelected: (BalanceRange) -> Unit
    ) {
        // rememberShimmerBrush must be called unconditionally (Compose rules).
        // The animation recomposition stays inside BalanceCard, not propagating up.
        val shimmerBrush = rememberShimmerBrush(showShimmer = isLoading)
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .shimmerEffect(shimmerBrush)
            )
        } else {
            var dropdownExpanded by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(PrimaryGradient)
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { dropdownExpanded = true }
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Text(
                                    text = selectedRange.displayName,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Select Range",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .size(16.dp)
                                )
                            }

                            MaterialTheme(
                                colorScheme = MaterialTheme.colorScheme.copy(
                                    surface = Color.Transparent
                                ),
                                shapes = MaterialTheme.shapes.copy(
                                    extraSmall = RoundedCornerShape(16.dp)
                                )
                            ) {
                                DropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false },
                                    modifier = Modifier
                                        .background(PrimaryGradient, shape = RoundedCornerShape(16.dp))
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(16.dp))
                                ) {
                                    BalanceRange.values().forEach { range ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = range.displayName,
                                                    color = if (range == selectedRange) Color.White else Color.White.copy(alpha = 0.7f),
                                                    fontWeight = if (range == selectedRange) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 14.sp
                                                )
                                            },
                                            onClick = {
                                                onRangeSelected(range)
                                                dropdownExpanded = false
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = Color.White,
                                                leadingIconColor = Color.White,
                                                trailingIconColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = "Options",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = balance.toCurrencyString(),
                        color = if (balance < 0) Color(0xFFFF8A80) else Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Column(horizontalAlignment = Alignment.Start) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDownward,
                                        contentDescription = "Income",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Income",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                modifier = Modifier,
                                text = income.toCurrencyString(),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }


                        Column(horizontalAlignment = Alignment.Start) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowUpward,
                                        contentDescription = "Expenses",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Expense",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                modifier = Modifier,
                                text = expense.toCurrencyString(),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TransactionHeader(
        isLoading: Boolean,
        onSeeAllClick: () -> Unit
    ) {
        // rememberShimmerBrush must be called unconditionally (Compose rules).
        // The animation only invalidates this composable's subtree — not the parent.
        val shimmerBrush = rememberShimmerBrush(showShimmer = isLoading)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .size(120.dp, 24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .shimmerEffect(shimmerBrush)
                )
                Box(
                    modifier = Modifier
                        .size(60.dp, 16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect(shimmerBrush)
                )
            } else {
                Text(
                    text = "Transactions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "See All",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onSeeAllClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }

    @Composable
    fun TransactionItem(
        transaction: Transaction,
        onBatchIconClick: (() -> Unit)? = null
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = transaction.icon,
                        contentDescription = transaction.title,
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = transaction.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (transaction.description?.contains("Batch scanned barcode") == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Rounded.Layers,
                                contentDescription = "Batched Barcode Item",
                                tint = Color(0xFF635BFF),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        onBatchIconClick?.invoke()
                                    }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = transaction.formattedDate.ifEmpty { DateTimeUtils.formatTimestamp(transaction.timestamp) },
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }

            val amountColor = if (transaction.amount > 0) PositiveGreen else NegativeRed
            val amountPrefix = if (transaction.amount > 0) "+$" else "-$"

            Text(
                text = "$amountPrefix${abs(transaction.amount).toInt()}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }

    @Composable
    fun rememberShimmerBrush(
        showShimmer: Boolean,
        colors: List<Color> = listOf(
            Color(0xFFEAEAEA),
            Color(0xFFF5F5F5),
            Color(0xFFEAEAEA),
        )
    ): Brush {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnimState = transition.animateFloat(
            initialValue = -300f,
            targetValue = 900f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerTranslate"
        )
        return if (showShimmer) {
            Brush.linearGradient(
                colors = colors,
                start = Offset(translateAnimState.value, 0f),
                end = Offset(translateAnimState.value + 300f, 300f)
            )
        } else {
            remember { Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)) }
        }
    }

    fun Modifier.shimmerEffect(brush: Brush): Modifier = this.drawBehind {
        drawRect(brush = brush)
    }

    @Suppress("DEPRECATION")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SwipeableTransactionItem(
        transaction: Transaction,
        onDeleteTransaction: (Transaction) -> Unit,
        onNavigateToAddTransaction: (String?) -> Unit,
        onBatchIconClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val density = LocalDensity.current
        val currentOnDeleteTransaction by rememberUpdatedState(onDeleteTransaction)
        val currentTransaction by rememberUpdatedState(transaction)

        // LazyColumn already gives each item a stable key via items(key = { it.id }).
        // A second key() wrapper here would create a duplicate composition key that
        // doesn't match LazyColumn's slot table, causing the dismiss state to be
        // re-created on every recompose.
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { dismissValue ->
                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                    currentOnDeleteTransaction(currentTransaction)
                    true
                } else {
                    false
                }
            },
            positionalThreshold = { totalDistance ->
                val minThreshold = with(density) { 56.dp.toPx() }
                maxOf(totalDistance * 0.6f, minThreshold)
            }
        )

        LaunchedEffect(dismissState.currentValue) {
            if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                kotlinx.coroutines.delay(1000)
                if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                    dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                }
            }
        }

        SwipeToDismissBox(
            modifier = modifier,
            state = dismissState,
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = true,
            backgroundContent = {
                val color = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFE53935)
                    else -> Color.Transparent
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(color)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BackgroundGray)
                        .clickable {
                            onNavigateToAddTransaction(transaction.id)
                        }
                ) {
                    TransactionItem(
                        transaction = transaction,
                        onBatchIconClick = onBatchIconClick
                    )
                }
            }
        )
    }

    @Composable
    fun ShimmerTransactionItem() {
        // The shimmer brush is created here, inside each ShimmerTransactionItem.
        // This scopes the 60fps infinite animation to only the shimmer items;
        // the parent LazyColumn and HomeScreenContent are NOT recomposed each frame.
        val shimmerBrush = rememberShimmerBrush(showShimmer = true)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .shimmerEffect(shimmerBrush)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Box(
                        modifier = Modifier
                            .size(120.dp, 16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect(shimmerBrush)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(80.dp, 12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect(shimmerBrush)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(60.dp, 18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(shimmerBrush)
            )
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun WalletDashboardPreview() {
        MaterialTheme {
            HomeScreenContent(
                innerPadding = PaddingValues(),
                uiState = HomeUiState(
                    transactions = listOf(
                        Transaction(
                            id = "1",
                            title = "Groceries",
                            timestamp = System.currentTimeMillis(),
                            amount = -150.0,
                            icon = Icons.Rounded.ShoppingBag,
                            iconBgColor = Color(0xFFF19E79),
                            type = TransactionType.EXPENSE,
                            category = com.hevincj.cashflow.domain.models.TransactionCategory.GROCERIES,
                            description = "Weekly shop",
                            isSynced = true
                        )
                    ),
                    totalBalance = 5000.0,
                    totalIncome = 6500.0,
                    totalExpense = 1500.0,
                    isLoading = false
                ),
                onRangeSelected = {},
                onDeleteTransaction = {},
                onRetrySync = {},
                onNavigateToAddTransaction = {},
                onNavigateToAllTransactions = {},
                rootNavController = rememberNavController()
            )
        }
    }

// Cached at file scope — DecimalFormat construction allocates locale data and is
// expensive. Safe to reuse from the main thread (all Compose calls are main-thread).
private val currencyFormatter = DecimalFormat("$#,##0.00")

private fun Double.toCurrencyString(): String = currencyFormatter.format(this)

private fun parseBatchDescription(description: String?): List<Pair<String, String>> {
    if (description == null || !description.contains("Batch scanned barcode")) return emptyList()
    val prefixIndex = description.indexOf(":")
    if (prefixIndex == -1) return emptyList()
    val detailsPart = description.substring(prefixIndex + 1).trim()
    val items = detailsPart.split(";").map { it.trim() }.filter { it.isNotEmpty() }
    val itemRegex = Regex("""(.+)\s*\((\d+)\)""")
    val parsedItems = mutableListOf<Pair<String, String>>()
    for (item in items) {
        val match = itemRegex.find(item)
        if (match != null) {
            val name = match.groupValues[1].trim()
            val code = match.groupValues[2].trim()
            parsedItems.add(Pair(name, code))
        }
    }
    return parsedItems
}

