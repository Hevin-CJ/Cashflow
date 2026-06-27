package com.hevincj.cashflow.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hevincj.cashflow.ui.theme.BackgroundGray
import com.hevincj.cashflow.ui.theme.BottomBarIconSelectedColor
import com.hevincj.cashflow.ui.theme.BottomBarIconUnselectedColor
import com.hevincj.cashflow.ui.theme.CardBackground
import com.hevincj.cashflow.ui.theme.PrimaryGradient
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.hilt.navigation.compose.hiltViewModel
import com.hevincj.cashflow.ui.screen.viewmodel.CardsViewModel
import com.hevincj.cashflow.ui.screen.viewmodel.HomeViewModel
import com.hevincj.cashflow.ui.screen.viewmodel.ProfileViewModel
import com.hevincj.cashflow.ui.screen.viewmodel.StatsViewModel
import com.hevincj.cashflow.ui.screen.viewmodel.ScanViewModel
import com.hevincj.cashflow.utils.SmoothNotchedShape

val LocalHomeViewModel = staticCompositionLocalOf<HomeViewModel?> { null }
val LocalStatsViewModel = staticCompositionLocalOf<StatsViewModel?> { null }
val LocalCardsViewModel = staticCompositionLocalOf<CardsViewModel?> { null }
val LocalProfileViewModel = staticCompositionLocalOf<ProfileViewModel?> { null }
val LocalScanViewModel = staticCompositionLocalOf<ScanViewModel?> { null }

@Composable
fun MainScreen(
    rootNavController: NavController
) {
    val navController = rememberNavController()
    val homeViewModel = LocalHomeViewModel.current ?: hiltViewModel()
    val statsViewModel = LocalStatsViewModel.current ?: hiltViewModel()
    val cardsViewModel = LocalCardsViewModel.current ?: hiltViewModel()
    val profileViewModel = LocalProfileViewModel.current ?: hiltViewModel()
    val scanViewModel = LocalScanViewModel.current ?: hiltViewModel()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundGray,
        bottomBar = { CustomBottomBarWithFab(navController = navController, rootNavController = rootNavController) },
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = "home_screen"
        ) {
            composable("home_screen") {
                HomeScreen(
                    innerPadding = innerPadding,
                    rootNavController = rootNavController,
                    viewModel = homeViewModel,
                    scanViewModel = scanViewModel
                )
            }
            composable("stats_screen") {
                StatisticsScreen(
                    innerPadding = innerPadding,
                    viewModel = statsViewModel
                )
            }
            composable("wallet_screen") {
                CardsScreen(
                    rootNavController = rootNavController,
                    innerPadding = innerPadding,
                    viewModel = cardsViewModel
                )
            }
            composable("profile_screen") {
                ProfileScreen(
                    rootNavController = rootNavController,
                    innerPaddingValues = innerPadding,
                    viewModel = profileViewModel
                )
            }
        }
    }
}

@Composable
private fun CustomBottomBarWithFab(navController: NavController, rootNavController: NavController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        BottomAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SmoothNotchedShape(cornerRadius = 16.dp, fabRadius = 32.dp, notchPadding = 8.dp)),
            containerColor = Color.Transparent,
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(CardBackground),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        navController.navigate("home_screen") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }, modifier = Modifier.weight(1f)) {
                        val tint = if (currentRoute == "home_screen") BottomBarIconSelectedColor else BottomBarIconUnselectedColor
                        Icon(Icons.Rounded.Home, contentDescription = "Home", tint = tint)
                    }

                    IconButton(onClick = {
                        navController.navigate("stats_screen") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }, modifier = Modifier.weight(1f)) {
                        val tint = if (currentRoute == "stats_screen") BottomBarIconSelectedColor else BottomBarIconUnselectedColor
                        Icon(Icons.Rounded.BarChart, contentDescription = "Stats", tint = tint)
                    }

                    Spacer(modifier = Modifier.width(80.dp))

                    IconButton(onClick = {
                        navController.navigate("wallet_screen") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }, modifier = Modifier.weight(1f)) {
                        val tint = if (currentRoute == "wallet_screen") BottomBarIconSelectedColor else BottomBarIconUnselectedColor
                        Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = "Wallet", tint = tint)
                    }

                    IconButton(onClick = {
                        navController.navigate("profile_screen") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }, modifier = Modifier.weight(1f)) {
                        val tint = if (currentRoute == "profile_screen") BottomBarIconSelectedColor else BottomBarIconUnselectedColor
                        Icon(Icons.Rounded.PersonOutline, contentDescription = "Profile", tint = tint)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { rootNavController.navigate("add_transaction") },
            shape = CircleShape,
            containerColor = Color.Transparent,
            elevation = FloatingActionButtonDefaults.elevation(8.dp, 4.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-20).dp)
                .size(58.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PrimaryGradient, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}