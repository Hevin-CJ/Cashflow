package com.hevincj.cashflow.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.hevincj.cashflow.ui.theme.CardBackground
import com.hevincj.cashflow.ui.theme.TextPrimary
import com.hevincj.cashflow.ui.theme.TextSecondary

import androidx.compose.runtime.Immutable

@Immutable
data class ContactInfo(
    val name: String,
    val phoneNumber: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpiContactPickerDialog(
    onDismissRequest: () -> Unit,
    onContactSelected: (name: String, phone: String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    
    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasContactsPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasContactsPermission) {
            launcher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    val contacts = remember(hasContactsPermission) {
        if (hasContactsPermission) {
            getPhoneContacts(context)
        } else {
            emptyList()
        }
    }

    val displayContacts = if (hasContactsPermission) contacts else emptyList()
    val filteredContacts = displayContacts.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f)
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Contact to Pay",
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

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or number") },
                    leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null, tint = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF9C27B0),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!hasContactsPermission) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Contacts permission is required to select contacts to pay. Please enable contacts permission in your device settings.",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                launcher.launch(Manifest.permission.READ_CONTACTS)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                        ) {
                            Text("Grant Permission", color = Color.White)
                        }
                    }
                } else if (filteredContacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No contacts found.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredContacts, key = { it.phoneNumber }) { contact ->
                            ContactRow(
                                contact = contact,
                                onClick = {
                                    // Pass phone number directly — caller will prompt for real UPI ID
                                    onContactSelected(contact.name, contact.phoneNumber)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactRow(
    contact: ContactInfo,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Contact Avatar / Initial Circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFE1BEE7)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.name.take(1).uppercase(),
                color = Color(0xFF4A148C),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Contact Info Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = contact.phoneNumber,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
    }
}

private fun getPhoneContacts(context: Context): List<ContactInfo> {
    val contactsList = mutableListOf<ContactInfo>()
    val contentResolver = context.contentResolver
    val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )
    
    try {
        val cursor = contentResolver.query(
            uri,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        cursor?.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            
            while (it.moveToNext()) {
                val name = if (nameIdx >= 0) it.getString(nameIdx) else "Unknown"
                val number = if (numberIdx >= 0) it.getString(numberIdx) else ""
                if (number.isNotBlank()) {
                    val cleanNumber = number.replace(Regex("[^0-9+]"), "")
                    contactsList.add(ContactInfo(name, cleanNumber))
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    val distinct = contactsList.distinctBy { it.phoneNumber }
    if (distinct.isEmpty()) {
        return listOf(
            ContactInfo("Alex Mercer", "+91 98765 43210"),
            ContactInfo("Bruce Wayne", "+91 88888 88888"),
            ContactInfo("Clark Kent", "+91 77777 77777"),
            ContactInfo("Diana Prince", "+91 66666 66666")
        )
    }
    return distinct
}
