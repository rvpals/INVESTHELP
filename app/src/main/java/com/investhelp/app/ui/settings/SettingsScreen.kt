package com.investhelp.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Preferences") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Data Management") }
                )
            }

            when (selectedTab) {
                0 -> PreferencesTab(viewModel, uiState)
                1 -> DataManagementTab(viewModel, uiState)
            }
        }
    }
}

@Composable
private fun PreferencesTab(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Transactions", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Auto-update position shares",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    "Automatically adjust position share count when saving a transaction (add on Buy, deduct on Sell)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = uiState.autoUpdateShares,
                onCheckedChange = { viewModel.setAutoUpdateShares(it) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("General", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Warn before delete",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    "Show a confirmation dialog before deleting items, transactions, transfers, or accounts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = uiState.warnBeforeDelete,
                onCheckedChange = { viewModel.setWarnBeforeDelete(it) }
            )
        }
    }
}

@Composable
private fun DataManagementTab(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    val context = LocalContext.current
    var showRestoreWarning by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.setBackupFolder(it)
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pendingRestoreUri = it
            showRestoreWarning = true
        }
    }

    if (showRestoreWarning) {
        AlertDialog(
            onDismissRequest = {
                showRestoreWarning = false
                pendingRestoreUri = null
            },
            title = { Text("Restore Data") },
            text = {
                Text("This will erase all current data and replace it with the backup. This action cannot be undone. Are you sure?")
            },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreWarning = false
                    pendingRestoreUri?.let { viewModel.restoreData(it) }
                    pendingRestoreUri = null
                }) {
                    Text("Restore", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreWarning = false
                    pendingRestoreUri = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Backup Folder", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = uiState.backupFolderName ?: "No folder selected",
            style = MaterialTheme.typography.bodyMedium,
            color = if (uiState.backupFolderName != null)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { folderPicker.launch(null) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select Backup Folder")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Export", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Save all data to a JSON file with a date-time stamp.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { viewModel.exportData() },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.backupFolderUri != null && !uiState.isExporting && !uiState.isRestoring
        ) {
            if (uiState.isExporting) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Export Data")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Restore", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Select a previously exported JSON file to restore. This will erase current data.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { filePicker.launch(arrayOf("application/json")) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isExporting && !uiState.isRestoring,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            if (uiState.isRestoring) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    color = MaterialTheme.colorScheme.onError,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Restore Data")
            }
        }
    }
}
