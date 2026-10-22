package com.metroos.launcher.apps

import android.Manifest
import android.content.pm.PackageManager
import android.telephony.SmsManager
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.metroos.launcher.ui.theme.MetroMagenta
import com.metroos.launcher.ui.theme.MetroTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SmsThread(val threadId: Long, val address: String, val snippet: String)
data class SmsMessage(val body: String, val timestamp: Long, val fromMe: Boolean)

/**
 * Messaging app: SMS thread list from Telephony.Sms, per-thread message
 * viewer and composer using SmsManager. Degrades gracefully without the
 * READ_SMS / SEND_SMS permissions.
 */
@Composable
fun MessagesApp() {
    val context = LocalContext.current
    var threads by remember { mutableStateOf<List<SmsThread>>(emptyList()) }
    var openThread by remember { mutableStateOf<SmsThread?>(null) }
    var permissionDenied by remember { mutableStateOf(false) }

    val smsPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) threads = loadThreads(context) else permissionDenied = true
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            threads = loadThreads(context)
        } else {
            smsPermission.launch(Manifest.permission.READ_SMS)
        }
    }

    openThread?.let { thread ->
        ConversationScreen(
            thread = thread,
            onBack = { openThread = null }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070E))
            .padding(horizontal = 20.dp)
    ) {
        Text(
            "Messages",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(vertical = 18.dp)
        )
        if (permissionDenied) {
            Text(
                "SMS permission was denied. Tap to request again.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                modifier = Modifier.clickable { smsPermission.launch(Manifest.permission.READ_SMS) }
            )
        } else if (threads.isEmpty()) {
            Text(
                "No conversations found.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(threads, key = { it.threadId }) { thread ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openThread = thread }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(thread.address, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            thread.snippet,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationScreen(thread: SmsThread, onBack: () -> Unit) {
    val context = LocalContext.current
    var messages by remember { mutableStateOf<List<SmsMessage>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var canSend by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val sendPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> canSend = granted }

    LaunchedEffect(thread.threadId) {
        messages = loadMessages(context, thread.threadId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070E))
            .imePadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.clickable(onClick = onBack)
            )
            Spacer(Modifier.width(12.dp))
            Text(thread.address, color = Color.White, fontSize = 18.sp)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { "${it.timestamp}_${it.body.hashCode()}" }) { msg ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .align(if (msg.fromMe) Alignment.CenterEnd else Alignment.CenterStart)
                            .background(
                                if (msg.fromMe) MetroTeal.copy(alpha = 0.9f)
                                else Color.White.copy(alpha = 0.12f)
                            )
                            .padding(12.dp)
                    ) {
                        Text(msg.body, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box {
                            if (draft.isEmpty()) {
                                Text("Text message", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                            }
                            inner()
                        }
                    }
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = Color.White,
                modifier = Modifier.clickable {
                    if (draft.isNotBlank()) {
                        if (!canSend) {
                            sendPermission.launch(Manifest.permission.SEND_SMS)
                        } else {
                            sendSms(context, thread.address, draft.trim())
                            messages = messages + SmsMessage(draft.trim(), System.currentTimeMillis(), true)
                            draft = ""
                        }
                    }
                }
            )
        }
    }
}

private fun loadThreads(context: android.content.Context): List<SmsThread> = runCatching {
    val out = mutableListOf<SmsThread>()
    val cursor = context.contentResolver.query(
        android.provider.Telephony.Sms.Conversations.CONTENT_URI,
        arrayOf(
            android.provider.Telephony.Sms.Conversations.THREAD_ID,
            android.provider.Telephony.Sms.Conversations.SNIPPET,
            android.provider.Telephony.Sms.Conversations.ADDRESS
        ),
        null, null,
        android.provider.Telephony.Sms.Conversations.DATE + " DESC"
    )
    cursor?.use { c ->
        val idIdx = c.getColumnIndexOrThrow(android.provider.Telephony.Sms.Conversations.THREAD_ID)
        val snipIdx = c.getColumnIndexOrThrow(android.provider.Telephony.Sms.Conversations.SNIPPET)
        val addrIdx = c.getColumnIndexOrThrow(android.provider.Telephony.Sms.Conversations.ADDRESS)
        while (c.moveToNext() && out.size < 100) {
            out.add(
                SmsThread(
                    threadId = c.getLong(idIdx),
                    address = c.getString(addrIdx) ?: "Unknown",
                    snippet = c.getString(snipIdx) ?: ""
                )
            )
        }
    }
    out
}.getOrDefault(emptyList())

private fun loadMessages(context: android.content.Context, threadId: Long): List<SmsMessage> = runCatching {
    val out = mutableListOf<SmsMessage>()
    val cursor = context.contentResolver.query(
        android.provider.Telephony.Sms.CONTENT_URI,
        arrayOf(android.provider.Telephony.Sms.BODY, android.provider.Telephony.Sms.DATE, android.provider.Telephony.Sms.TYPE),
        "${android.provider.Telephony.Sms.THREAD_ID} = ?",
        arrayOf(threadId.toString()),
        android.provider.Telephony.Sms.DATE + " ASC"
    )
    cursor?.use { c ->
        val bodyIdx = c.getColumnIndexOrThrow(android.provider.Telephony.Sms.BODY)
        val dateIdx = c.getColumnIndexOrThrow(android.provider.Telephony.Sms.DATE)
        val typeIdx = c.getColumnIndexOrThrow(android.provider.Telephony.Sms.TYPE)
        while (c.moveToNext() && out.size < 200) {
            out.add(
                SmsMessage(
                    body = c.getString(bodyIdx) ?: "",
                    timestamp = c.getLong(dateIdx),
                    // MESSAGE_TYPE_SENT (2) / MESSAGE_TYPE_SENT (1) = own messages
                    fromMe = c.getInt(typeIdx) != android.provider.Telephony.Sms.MESSAGE_TYPE_INBOX
                )
            )
        }
    }
    out
}.getOrDefault(emptyList())

private fun sendSms(context: android.content.Context, address: String, body: String) {
    runCatching {
        @Suppress("DEPRECATION")
        val manager = SmsManager.getDefault()
        manager.sendTextMessage(address, null, body, null, null)
    }
}
