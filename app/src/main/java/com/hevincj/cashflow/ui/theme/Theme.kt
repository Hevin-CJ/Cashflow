package com.hevincj.cashflow.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalDarkTheme = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFFF3F4F6),
    onSurface = Color(0xFFF3F4F6)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1F2937),
    onSurface = Color(0xFF1F2937)
)

val GradientLightBlue: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF29B6F6) else Color(0xFF4FC3F7)

val GradientPurple: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFFB39DDB) else Color(0xFF9575CD)

val GradientOrange: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFFFFAB91) else Color(0xFFFF8A65)

val BackgroundGray: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF121212) else Color(0xFFFAFAFA)

val TextPrimary: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFFF3F4F6) else Color(0xFF1F2937)

val TextSecondary: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)

val PositiveGreen: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF34D399) else Color(0xFF10B981)

val NegativeRed: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFFF87171) else Color(0xFFEF4444)

val CardBackground: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF1E1E1E) else Color.White

val PrimaryGradient: Brush
    @Composable
    get() = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF64B5F6),
                Color(0xFF9575CD),
                Color(0xFFFF8A65)
            )
        )
    }

@Composable
fun CashFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}