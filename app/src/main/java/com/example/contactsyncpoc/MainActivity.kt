package com.example.contactsyncpoc

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.example.contactsyncpoc.ui.theme.ContactSyncPocTheme
import com.example.contactsyncpoc.worker.ContactSyncWorker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
class MainActivity : ComponentActivity() {

    private var onPermissionResult: ((Boolean) -> Unit)? = null

    private val requestContactsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            onPermissionResult?.invoke(isGranted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ContactSyncPocTheme {
                var showConsentDialog by remember { mutableStateOf(value = false) }
                var syncStatus by remember { mutableStateOf(value = "Press Start Sync") }

                // Listen for permission result globally for the session, capturing UI state
                onPermissionResult = { granted ->
                    if (granted) {
                        startSyncWorker { newStatus -> syncStatus = newStatus }
                    } else {
                        syncStatus = "Consent denied"
                    }
                }

                ContactSyncScreen(
                    statusText = syncStatus,
                    onStartSyncClick = {
                        checkContactsPermission(
                            onShowRationale = { showConsentDialog = true },
                            onPermissionGranted = {
                                startSyncWorker { newStatus -> syncStatus = newStatus }
                            }
                        )
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
    onStartSyncClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = onStartSyncClick,
        ) {
            Text("Start Sync")
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
