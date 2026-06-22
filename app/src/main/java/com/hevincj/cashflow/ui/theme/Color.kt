package com.hevincj.cashflow.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)


val PhoneBackgroundColor = Color(0xFFEDE8FF)

val AppCardBackgroundColor: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF1E1E1E) else Color.White

val GridIconContainerColor: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF2C2C2E) else Color(0xFFEEEEEE)

val IconBackgroundColor1 = Color(0xFFFFD6D6)
val IconBackgroundColor2 = Color(0xFFD1EDFF)
val IncomePurpleColor = Color(0xFF8121FD)
val ExpenseOrangeColor = Color(0xFFFF5722)

val TextPrimaryColor: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFFF3F4F6) else Color(0xFF212121)

val TextSecondaryColor: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF8E8E8E)

val NegativeAmountColor = Color(0xFFE53935)

val ChartLabelColor: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF8E8E8E)

val ChartGridLineColor: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF2C2C2E) else Color(0xFFCCCCCC)

val TabUnselectedColor: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF2C2C2E) else Color(0xFFEEEEEE)

val TabSelectedColor = Color(0xFFFF5722)
val BottomBarIconUnselectedColor = Color(0xFF8E8E8E)
val BottomBarIconSelectedColor = Color(0xFF8121FD)
val FABBackgroundColor = Color(0xFF8121FD)



val DarkBackground = Color(0xFF000000)
val ButtonSurfaceDark = Color(0xFF1A1A1C)
val TextGraySubtle = Color(0xFF8E8E93)
val TextGrayDark = Color(0xFF555555)

val primaryGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFBA68C8), Color(0xFFF78B00))
)

val bottomBarGradient = primaryGradient

val AccentOrange = Color(0xFFF78B00)