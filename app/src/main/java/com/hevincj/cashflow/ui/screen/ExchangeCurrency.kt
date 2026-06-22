package com.hevincj.cashflow.ui.screen


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hevincj.cashflow.ui.theme.AccentOrange
import com.hevincj.cashflow.ui.theme.TextPrimary
import com.hevincj.cashflow.ui.theme.BackgroundGray
import com.hevincj.cashflow.ui.theme.LocalDarkTheme
import com.hevincj.cashflow.ui.theme.DarkBackground
import com.hevincj.cashflow.ui.theme.GridIconContainerColor
import com.hevincj.cashflow.ui.theme.PhoneBackgroundColor
import com.hevincj.cashflow.ui.theme.PrimaryGradient
import com.hevincj.cashflow.ui.theme.TextGrayDark
import com.hevincj.cashflow.ui.theme.TextGraySubtle


@Composable
fun ExchangeRateScreen(innerPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(BackgroundGray)
            .padding(vertical = 16.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            Text(
                text = "Exchange Currency",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        CurrencyDisplayBlock(
            currencyName = "US Dollar",
            currencyCode = "USD",
            value = "849",
            isEditing = false
        )

        Spacer(modifier = Modifier.height(16.dp))

        CurrencyDisplayBlock(
            currencyName = "Indian Rupee",
            currencyCode = "INR",
            value = "78,510.8545",
            isEditing = true
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            modifier = Modifier
                .fillMaxWidth()
            ,
            text = "Data source: xCurrency. Last updated: Apr 17, 2026 9:40:20 PM",
            color = TextGrayDark,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))


        CustomNumpad(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp) // Fixed height to maintain perfect circular ratios
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun CurrencyDisplayBlock(
    currencyName: String,
    currencyCode: String,
    value: String,
    isEditing: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = currencyName,
                color = TextPrimary,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = currencyCode,
                color = TextGraySubtle,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            SurfaceIcon(icon = Icons.Rounded.ChevronRight)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 40.sp,
                fontWeight = FontWeight.Normal
            )
            if (isEditing) {

                Box(
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .width(3.dp)
                        .height(40.dp)
                        .background(AccentOrange)
                )
            }
        }
    }
}

@Composable
private fun CustomNumpad(modifier: Modifier = Modifier) {
    Row(modifier = modifier) {

        Column(modifier = Modifier.weight(3f)) {
            Row(modifier = Modifier.weight(1f)) {
                CalcButton(text = "AC", textColor = AccentOrange, modifier = Modifier.weight(1f))
                CalcIcon(icon = Icons.AutoMirrored.Rounded.Backspace, tint = AccentOrange, modifier = Modifier.weight(1f))
                CalcButton(text = "×", textColor = AccentOrange, modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.weight(1f)) {
                CalcButton(text = "7", modifier = Modifier.weight(1f))
                CalcButton(text = "8", modifier = Modifier.weight(1f))
                CalcButton(text = "9", modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.weight(1f)) {
                CalcButton(text = "4", modifier = Modifier.weight(1f))
                CalcButton(text = "5", modifier = Modifier.weight(1f))
                CalcButton(text = "6", modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.weight(1f)) {
                CalcButton(text = "1", modifier = Modifier.weight(1f))
                CalcButton(text = "2", modifier = Modifier.weight(1f))
                CalcButton(text = "3", modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.weight(1f)) {
                CalcButton(text = "00", modifier = Modifier.weight(1f))
                CalcButton(text = "0", modifier = Modifier.weight(1f))
                CalcButton(text = ".", modifier = Modifier.weight(1f))
            }
        }


        Column(modifier = Modifier.weight(1f)) {
            CalcButton(text = "÷", textColor = AccentOrange, modifier = Modifier.weight(1f))
            CalcButton(text = "-", textColor = AccentOrange, modifier = Modifier.weight(1f))
            CalcButton(text = "+", textColor = AccentOrange, modifier = Modifier.weight(1f))


            Box(
                modifier = Modifier
                    .weight(2f)
                    .padding(8.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(100.dp))
                    .background(PrimaryGradient)
                    .clickable { /* Handle Equals */ },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "=",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


@Composable
private fun CalcButton(
    text: String,
    textColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val resolvedTextColor = if (textColor == Color.Unspecified) TextPrimary else textColor
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
            .clip(CircleShape)
            .background(GridIconContainerColor)
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = resolvedTextColor,
            textAlign = TextAlign.Center,
            fontSize = 28.sp,
            fontWeight = FontWeight.Normal,
            style = TextStyle(
                platformStyle = PlatformTextStyle(
                    includeFontPadding = false
                ),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                )
            )
        )
    }
}

@Composable
private fun CalcIcon(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
            .clip(CircleShape)
            .background(GridIconContainerColor)
            .clickable { },
        contentAlignment = Alignment.Center // Mathematically locks icon to the center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun SurfaceIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(PrimaryGradient),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PhoneBackgroundColor,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ExchangeRateScreenPreview() {
    MaterialTheme {
        ExchangeRateScreen(PaddingValues())
    }
}