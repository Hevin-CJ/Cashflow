package com.hevincj.cashflow.ui.screen

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.hevincj.cashflow.ui.theme.CardBackground
import com.hevincj.cashflow.ui.theme.TextPrimary
import com.hevincj.cashflow.ui.theme.TextSecondary

// ─── Data class for discovered UPI apps ──────────────────────────────────────

private data class UpiAppInfo(
    val name: String,
    val packageName: String,
    val icon: ImageBitmap?
)

// ─── Known UPI apps with their deep-link balance check URIs ──────────────────

private val knownUpiApps = listOf(
    Triple("Google Pay",  "com.google.android.apps.nbu.paisa.user", "upi://pay"),
    Triple("PhonePe",     "com.phonepe.app",                        "phonepe://main"),
    Triple("Paytm",       "net.one97.paytm",                        "paytmmp://balance"),
    Triple("BHIM",        "in.org.npci.upiapp",                     "upi://pay"),
    Triple("Amazon Pay",  "in.amazon.mShop.android.shopping",       "amzn://apps/android"),
    Triple("MobiKwik",    "com.mobikwik_new",                       "mobikwik://main"),
    Triple("FreeCharge",  "com.freecharge.android",                 "freecharge://main"),
    Triple("Airtel Money","com.myairtelapp",                        "airtel://main"),
    Triple("SBI YONO",    "com.sbi.lotusintouch",                   null),
    Triple("HDFC MobileBanking", "com.snapwork.HDFC",              null),
    Triple("ICICI iMobile","com.csam.icici.bank.imobile",          null),
    Triple("Axis Mobile", "com.axis.mobile",                        null)
)

// ─── Main Dialog ──────────────────────────────────────────────────────────────

@Composable
fun UpiCheckBalanceDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    // Discover installed UPI / banking apps
    val installedApps: List<UpiAppInfo> = remember {
        val pm = context.packageManager
        knownUpiApps.mapNotNull { (name, pkg, _) ->
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val drawable = pm.getApplicationIcon(appInfo)
                val bitmap = try { drawable.toBitmap().asImageBitmap() } catch (e: Exception) { null }
                UpiAppInfo(name, pkg, bitmap)
            } catch (e: PackageManager.NameNotFoundException) {
                null // App not installed — skip
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Header ────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Check Balance",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Open a UPI or banking app on your phone to check your balance.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                if (installedApps.isEmpty()) {
                    // ── No apps found ────────────────────────────────────
                    EmptyAppsPlaceholder()
                } else {
                    // ── App list ─────────────────────────────────────────
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(installedApps) { app ->
                            AppLaunchRow(app = app) {
                                // Try deep link first, then plain launcher intent
                                val deepLinkUri = knownUpiApps
                                    .firstOrNull { it.second == app.packageName }
                                    ?.third

                                var launched = false

                                if (deepLinkUri != null) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkUri)).apply {
                                            setPackage(app.packageName)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                        launched = true
                                    } catch (e: Exception) {
                                        // Deep link failed — fall through to launcher
                                    }
                                }

                                if (!launched) {
                                    try {
                                        val launchIntent = context.packageManager
                                            .getLaunchIntentForPackage(app.packageName)
                                            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                        if (launchIntent != null) context.startActivity(launchIntent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }

                                onDismissRequest()
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── USSD fallback option ──────────────────────────────────
                OutlinedButton(
                    onClick = {
                        try {
                            // NPCI USSD balance check — works on any SIM without internet
                            val ussdCode = "*99#"
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(ussdCode)}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Use USSD (*99#) — No Internet needed",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ─── Single app row ───────────────────────────────────────────────────────────

@Composable
private fun AppLaunchRow(app: UpiAppInfo, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        targetValue = if (isPressed) Color.White.copy(alpha = 0.08f) else Color.Transparent,
        animationSpec = tween(150),
        label = "row_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                1.dp,
                Color.White.copy(alpha = 0.07f),
                RoundedCornerShape(14.dp)
            )
            .clickable {
                isPressed = true
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // App icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                if (app.icon != null) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = app.name,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalance,
                        contentDescription = app.name,
                        tint = Color(0xFF9C27B0),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // App name
            Text(
                text = app.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        // Open arrow
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = "Open app",
            tint = Color(0xFF9C27B0),
            modifier = Modifier.size(18.dp)
        )
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyAppsPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFF9C27B0).copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AccountBalance,
                contentDescription = null,
                tint = Color(0xFF9C27B0),
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No UPI apps found",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Install GPay, PhonePe, or Paytm to check balance from here. You can still use the USSD option below.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
