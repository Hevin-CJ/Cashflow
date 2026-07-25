package com.hevincj.cashflow.ui.screen

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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

// ─── Known UPI apps that support payment by phone/VPA deep links ─────────────
private val upiPaymentApps = listOf(
    Triple("Google Pay",   "com.google.android.apps.nbu.paisa.user", "tez"),
    Triple("PhonePe",      "com.phonepe.app",                        "phonepe"),
    Triple("Paytm",        "net.one97.paytm",                        "paytmmp"),
    Triple("BHIM",         "in.org.npci.upiapp",                     "upi"),
    Triple("Amazon Pay",   "in.amazon.mShop.android.shopping",       "amazon"),
    Triple("MobiKwik",     "com.mobikwik_new",                       "mobikwik"),
    Triple("SBI YONO",     "com.sbi.lotusintouch",                   null),
    Triple("HDFC Mobile",  "com.snapwork.HDFC",                      null),
    Triple("ICICI iMobile","com.csam.icici.bank.imobile",            null),
    Triple("Axis Mobile",  "com.axis.mobile",                        null)
)

// ─── Internal data class ──────────────────────────────────────────────────────
private data class UpiPayApp(
    val name: String,
    val packageName: String,
    val icon: ImageBitmap?
)

// ─── Phone number normalisation for UPI ──────────────────────────────────────
/**
 * Strips formatting and country code, returns a 10-digit Indian mobile number,
 * or the original digits if we can't normalise.
 *
 * Examples:
 *   "+91 98765 43210" → "9876543210"
 *   "09876543210"     → "9876543210"
 *   "9876543210"      → "9876543210"
 */
private fun normalisePhone(raw: String): String {
    val digits = raw.replace(Regex("[^0-9]"), "")
    return when {
        digits.startsWith("91") && digits.length == 12 -> digits.substring(2)
        digits.startsWith("0") && digits.length == 11  -> digits.substring(1)
        digits.length == 10                             -> digits
        else                                            -> digits
    }
}

// ─── Main Dialog ──────────────────────────────────────────────────────────────

/**
 * Shows the UPI apps installed on the device.
 * When the user taps one, opens it with a UPI payment intent pre-filled
 * for [contactPhone] (as `<phone>@upi` VPA) and [contactName].
 */
@Composable
fun UpiAppPickerForContactDialog(
    contactName: String,
    contactPhone: String,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val phone10 = remember(contactPhone) { normalisePhone(contactPhone) }

    // Discover installed UPI payment apps
    val installedApps: List<UpiPayApp> = remember {
        val pm = context.packageManager
        upiPaymentApps.mapNotNull { (name, pkg, _) ->
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val bmp = try {
                    pm.getApplicationIcon(appInfo).toBitmap().asImageBitmap()
                } catch (e: Exception) { null }
                UpiPayApp(name, pkg, bmp)
            } catch (e: PackageManager.NameNotFoundException) {
                null
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
                    Column {
                        Text(
                            text = "Pay ${contactName.substringBefore(" ")}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = contactPhone,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Choose a UPI app to complete the payment",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                if (installedApps.isEmpty()) {
                    // ── No UPI apps ───────────────────────────────────────
                    NoUpiAppsState()
                } else {
                    // ── App list ─────────────────────────────────────────
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(installedApps, key = { it.packageName }) { app ->
                            UpiPayAppRow(app = app) {
                                launchUpiPayment(
                                    context = context,
                                    packageName = app.packageName,
                                    phone = phone10,
                                    name = contactName
                                )
                                onDismissRequest()
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── System UPI chooser fallback ───────────────────────────
                OutlinedButton(
                    onClick = {
                        launchSystemUpiChooser(context, phone10, contactName)
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f))
                        )
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open system UPI chooser",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ─── Launch helpers ───────────────────────────────────────────────────────────

/**
 * Launches a specific UPI app with a pre-filled payment intent.
 * Uses `<phone10digit>@upi` as the VPA — most UPI apps resolve this to the
 * registered virtual payment address for that mobile number.
 */
private fun launchUpiPayment(
    context: android.content.Context,
    packageName: String,
    phone: String,
    name: String
) {
    val vpa = "$phone@upi"
    val upiUri = buildUpiUri(vpa, name)

    var launched = false

    // Try a targeted UPI intent first
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUri)).apply {
            setPackage(packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        launched = true
    } catch (e: Exception) {
        // Deep link rejected — fall back to launcher
    }

    if (!launched) {
        try {
            val launchIntent = context.packageManager
                .getLaunchIntentForPackage(packageName)
                ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            if (launchIntent != null) context.startActivity(launchIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Shows the Android system app chooser for all apps that handle `upi://`.
 * This is the safest fallback when no specific app is selected.
 */
private fun launchSystemUpiChooser(
    context: android.content.Context,
    phone: String,
    name: String
) {
    val vpa = "$phone@upi"
    val upiUri = buildUpiUri(vpa, name)
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUri)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Pay $name via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/** Builds a standard UPI payment URI with VPA and payee name. */
private fun buildUpiUri(vpa: String, name: String): String {
    val encodedName = Uri.encode(name)
    return "upi://pay?pa=${Uri.encode(vpa)}&pn=$encodedName&cu=INR"
}

// ─── UI Components ────────────────────────────────────────────────────────────

@Composable
private fun UpiPayAppRow(app: UpiPayApp, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        targetValue = if (isPressed) Color.White.copy(alpha = 0.08f) else Color.Transparent,
        animationSpec = tween(150),
        label = "pay_row_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
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
            Text(
                text = app.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Pay",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF9C27B0)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = "Open",
                tint = Color(0xFF9C27B0),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun NoUpiAppsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFF9C27B0).copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AccountBalance,
                contentDescription = null,
                tint = Color(0xFF9C27B0),
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No UPI apps found",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Install GPay, PhonePe, or Paytm to pay directly. Try the system chooser below.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
