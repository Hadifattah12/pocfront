package com.example.contactsyncpoc

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkInfo
import androidx.lifecycle.Observer
import com.example.contactsyncpoc.data.AppDatabase
import com.example.contactsyncpoc.data.ContactSnapshot
import com.example.contactsyncpoc.ui.theme.ContactSyncPocTheme
import com.example.contactsyncpoc.worker.ContactSyncWorker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.background

class MainActivity : ComponentActivity() {

    private var onPermissionResult: ((Boolean) -> Unit)? = null

    private val requestContactsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission())
    {
        isGranted -> onPermissionResult?.invoke(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        setContent {
            ContactSyncPocTheme {
                var currentScreen by remember { mutableStateOf("HOME") }
                var selectedContact by remember { mutableStateOf<ContactSnapshot?>(null) }

                var showConsentDialog by remember { mutableStateOf(value = false) }
                var syncStatus by remember { mutableStateOf(value = "Press Start Sync") }

                // Listen for permission result globally for the session, capturing UI state
                onPermissionResult = { granted ->
                    if (granted)
                    {
                        startSyncWorker { newStatus -> syncStatus = newStatus }
                    }
                    else
                    {
                        syncStatus = "Consent denied"
                    }
                }

                when(currentScreen) {
                    "HOME" -> {
                        ContactSyncScreen(
                            statusText = syncStatus,
                            onStartSyncClick = {
                                checkContactsPermission(
                                    onShowRationale = { showConsentDialog = true },
                                    onPermissionGranted = {
                                        startSyncWorker { newStatus -> syncStatus = newStatus }
                                    }
                                )
                            },
                            onOpenChatsClick = {
                                currentScreen = "CHAT_LIST"
                            }
                        )

                        if (showConsentDialog) {
                            ConsentDialog(
                                onAllow = {
                                    showConsentDialog = false
                                    requestContactsPermission.launch(Manifest.permission.READ_CONTACTS)
                                },
                                onDismiss = {
                                    showConsentDialog = false
                                    syncStatus = "Consent denied"
                                }
                            )
                        }
                    }
                    "CHAT_LIST" -> {
                        ChatListScreen(
                            onBack = { currentScreen = "HOME" },
                            onContactClick = { contact ->
                                selectedContact = contact
                                currentScreen = "CHAT_DETAIL"
                            }
                        )
                    }
                    "CHAT_DETAIL" -> {
                        selectedContact?.let { contact ->
                            ChatDetailScreen(
                                contact = contact,
                                onBack = { currentScreen = "CHAT_LIST" }
                            )
                        } ?: run {
                            currentScreen = "CHAT_LIST"
                        }
                    }
                }
            }
        }

    }

    private fun checkContactsPermission(onShowRationale: () -> Unit, onPermissionGranted: () -> Unit) {
        val permission = Manifest.permission.READ_CONTACTS

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted()
        } else {
            onShowRationale()
        }
    }

    private fun startSyncWorker(onStatusChange: (String) -> Unit) {
        val syncRequest = OneTimeWorkRequestBuilder<ContactSyncWorker>().build()
        val workManager = WorkManager.getInstance(this)
        workManager.enqueue(syncRequest)

        workManager.getWorkInfoByIdLiveData(syncRequest.id).observe(this, Observer { workInfo ->
            if (workInfo != null) {
                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED -> onStatusChange("Sync starts shortly...")
                    WorkInfo.State.RUNNING -> onStatusChange("Sync in progress...")
                    WorkInfo.State.SUCCEEDED -> onStatusChange("Sync completed successfully!")
                    WorkInfo.State.FAILED -> onStatusChange("Sync failed.")
                    WorkInfo.State.CANCELLED -> onStatusChange("Sync cancelled.")
                    WorkInfo.State.BLOCKED -> onStatusChange("Sync starts shortly...")
                }
            }
        })
    }
}

@Composable
fun ContactSyncScreen(
    statusText: String,
    onStartSyncClick: () -> Unit,
    onOpenChatsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = onStartSyncClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Sync")
        }

        Button(
            onClick = onOpenChatsClick,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Open Chats")
        }

        Text(
            text = statusText,
            modifier = Modifier.padding(top = 24.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun ConsentDialog(
    onAllow: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contact Access") },
        text = { Text("We use your contacts to help you find friends on Whish. This ensures transparency and helps you connect with your community.") },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text("Allow")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now")
            }
        }
    )
}

@Composable
fun FakeTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(16.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = "<-",
            modifier = Modifier
                .clickable { onBack() }
                .padding(end = 16.dp, top = 4.dp, bottom = 4.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun ChatListScreen(
    onBack: () -> Unit,
    onContactClick: (ContactSnapshot) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var contacts by remember { mutableStateOf<List<ContactSnapshot>>(emptyList()) }

    LaunchedEffect(Unit) {
        contacts = AppDatabase.getDatabase(context).contactDao().getAllContacts()
    }

    Column(modifier = Modifier.fillMaxSize())
    {
        FakeTopBar(title = "Chats", onBack = onBack)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(contacts)
            { contact ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onContactClick(contact) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text(
                            text = (contact.displayName?.take(1) ?: "U").uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contact.displayName ?: "Unknown",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = contact.phoneNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.ui.graphics.Color.Gray
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 80.dp)
                        .height(1.dp)
                        .background(androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.5f))
                )
            }
            if (contacts.isEmpty()) {
                item {
                    Text(
                        "No contacts found. Have you synced yet?",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatDetailScreen(
    contact: ContactSnapshot,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFFEFE6DD))) {
        FakeTopBar(title = contact.displayName ?: contact.phoneNumber, onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = androidx.compose.ui.graphics.Color.White,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text("Hi! Yeah, I just joined the app.", color = androidx.compose.ui.graphics.Color.Black)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = androidx.compose.ui.graphics.Color(0xFFDCF8C6),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text("Hey there!", color = androidx.compose.ui.graphics.Color.Black)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(androidx.compose.ui.graphics.Color.Transparent)
                .padding(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            var fakeInput by remember { mutableStateOf("") }
            androidx.compose.material3.OutlinedTextField(
                value = fakeInput,
                onValueChange = { fakeInput = it },
                modifier = Modifier.weight(1f).background(androidx.compose.ui.graphics.Color.White, androidx.compose.foundation.shape.RoundedCornerShape(24.dp)),
                placeholder = { Text("Message") },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)
                    .clickable { fakeInput = "" },
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(">", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
