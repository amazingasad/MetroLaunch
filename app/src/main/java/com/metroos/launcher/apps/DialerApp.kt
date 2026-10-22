package com.metroos.launcher.apps

import android.Manifest
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.metroos.launcher.ui.theme.MetroGreen
import com.metroos.launcher.ui.theme.MetroTeal

/**
 * Phone dialer: numeric keypad, contacts via ContactsContract, and recent
 * calls via CallLog. Places calls through ACTION_CALL when CALL_PHONE is
 * granted, otherwise falls back to the system dialer via ACTION_DIAL.
 */
@Composable
fun DialerApp() {
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var number by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var callLog by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    val contactsPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) contacts = loadContacts(context)
        callLog = if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG)
            == PackageManager.PERMISSION_GRANTED
        ) loadCallLog(context) else callLog
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            contacts = loadContacts(context)
        } else {
            contactsPermission.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF05070E))) {
        TabRow(
            selectedTabIndex = tab,
            containerColor = Color(0xFF0E1420),
            contentColor = Color.White
        ) {
            listOf("Keypad", "Contacts", "Recents").forEachIndexed { index, title ->
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    text = { Text(title, fontSize = 13.sp) }
                )
            }
        }

        when (tab) {
            0 -> KeypadPane(
                number = number,
                onDigit = { number += it },
                onBackspace = { if (number.isNotEmpty()) number = number.dropLast(1) },
                onCall = { placeCall(context, number) }
            )
            1 -> ContactsPane(contacts, onCall = { placeCall(context, it) })
            else -> ContactsPane(callLog, onCall = { placeCall(context, it) })
        }
    }
}

@Composable
private fun KeypadPane(
    number: String,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onCall: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            number.ifEmpty { "Enter number" },
            color = if (number.isEmpty()) Color.White.copy(alpha = 0.4f) else Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(Modifier.height(28.dp))
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("*", "0", "#")
        )
        keys.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                row.forEach { digit ->
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.White.copy(alpha = 0.10f), CircleShape)
                            .clickable { onDigit(digit) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(digit, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light)
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MetroGreen, CircleShape)
                    .clickable(onClick = onCall),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Call, contentDescription = "Call", tint = Color.White)
            }
            Spacer(Modifier.width(28.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Backspace",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.clickable(onClick = onBackspace)
            )
        }
    }
}

@Composable
private fun ContactsPane(people: List<Pair<String, String>>, onCall: (String) -> Unit) {
    if (people.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No entries or permission missing",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        items(people, key = { it.first + it.second }) { (name, phone) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCall(phone) }
                    .padding(vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MetroTeal.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(name.take(1), color = Color.White, fontSize = 16.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(name, color = Color.White, fontSize = 15.sp)
                    Text(phone, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        }
    }
}

private fun loadContacts(context: android.content.Context): List<Pair<String, String>> = runCatching {
    val out = mutableListOf<Pair<String, String>>()
    val cursor = context.contentResolver.query(
        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
        ),
        null, null,
        android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
    )
    cursor?.use { c ->
        val nameIdx = c.getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numIdx = c.getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (c.moveToNext() && out.size < 200) {
            out.add(c.getString(nameIdx) to (c.getString(numIdx) ?: ""))
        }
    }
    out
}.getOrDefault(emptyList())

private fun loadCallLog(context: android.content.Context): List<Pair<String, String>> = runCatching {
    val out = mutableListOf<Pair<String, String>>()
    val cursor = context.contentResolver.query(
        CallLog.Calls.CONTENT_URI,
        arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER),
        null, null,
        CallLog.Calls.DATE + " DESC"
    )
    cursor?.use { c ->
        val nameIdx = c.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
        val numIdx = c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
        while (c.moveToNext() && out.size < 100) {
            val name = c.getString(nameIdx).orEmpty().ifEmpty { c.getString(numIdx) ?: "Unknown" }
            out.add(name to (c.getString(numIdx) ?: ""))
        }
    }
    out
}.getOrDefault(emptyList())

private fun placeCall(context: android.content.Context, number: String) {
    if (number.isBlank()) return
    val canDirect = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
        PackageManager.PERMISSION_GRANTED
    val intent = if (canDirect) {
        android.content.Intent(android.content.Intent.ACTION_CALL, android.net.Uri.parse("tel:$number"))
    } else {
        android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:$number"))
    }
    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
