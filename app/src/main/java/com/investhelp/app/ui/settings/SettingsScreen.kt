package com.investhelp.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
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
            .verticalScroll(rememberScrollState())
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

        Spacer(modifier = Modifier.height(24.dp))

        Text("Dashboard Market Indices", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            "Choose which market indices to show on the dashboard",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsViewModel.AVAILABLE_MARKET_INDICES.forEach { index ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${index.label} (${index.symbol})",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = uiState.enabledMarketIndices.contains(index.symbol),
                    onCheckedChange = { viewModel.toggleMarketIndex(index.symbol, it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataManagementTab(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    val context = LocalContext.current
    var showRestoreWarning by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var csvFileUri by remember { mutableStateOf<Uri?>(null) }

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

    val csvPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            csvFileUri = it
            viewModel.parseCsvFile(it)
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

    // CSV mapping dialog
    uiState.csvImport?.let { csvState ->
        if (csvState.isImporting) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Importing...") },
                text = {
                    Column {
                        Text("${csvState.importCurrent} / ${csvState.importTotal} rows")
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { csvState.importProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {}
            )
        } else {
            CsvMappingDialog(
                csvState = csvState,
                accounts = uiState.accounts,
                onMappingChanged = { colIndex, field -> viewModel.updateCsvMapping(colIndex, field) },
                onAccountChanged = { viewModel.setCsvImportAccount(it) },
                onDismiss = { viewModel.dismissCsvImport() },
                onImport = { csvFileUri?.let { viewModel.executeCsvImport(it) } }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // --- Import Data ---
        Text("Import Data", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Import a CSV file to update the positions in the app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { csvPicker.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Import Position CSV")
        }

        Spacer(modifier = Modifier.height(32.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(32.dp))

        // --- Backup Folder ---
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CsvMappingDialog(
    csvState: CsvImportState,
    accounts: List<com.investhelp.app.data.local.entity.InvestmentAccountEntity>,
    onMappingChanged: (Int, String) -> Unit,
    onAccountChanged: (Long) -> Unit,
    onDismiss: () -> Unit,
    onImport: () -> Unit
) {
    val fields = SettingsViewModel.IMPORTABLE_FIELDS
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Map CSV Columns") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Account selector
                Text("Target Account", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = accountDropdownExpanded,
                    onExpandedChange = { accountDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = accounts.find { it.id == csvState.selectedAccountId }?.name ?: "Select account",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = accountDropdownExpanded,
                        onDismissRequest = { accountDropdownExpanded = false }
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    onAccountChanged(account.id)
                                    accountDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text("Column Mappings", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Map each CSV column to an app field. Preview shows first 3 rows.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable mapping table
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    csvState.csvHeaders.forEachIndexed { colIndex, header ->
                        CsvColumnCard(
                            header = header,
                            previewValues = csvState.previewRows.map { row ->
                                row.getOrElse(colIndex) { "" }
                            },
                            selectedField = csvState.columnMappings[colIndex] ?: "Skip",
                            fields = fields,
                            onFieldSelected = { onMappingChanged(colIndex, it) }
                        )
                        if (colIndex < csvState.csvHeaders.lastIndex) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onImport,
                enabled = csvState.columnMappings.containsValue("ticker") && csvState.selectedAccountId != -1L
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CsvColumnCard(
    header: String,
    previewValues: List<String>,
    selectedField: String,
    fields: List<String>,
    onFieldSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.width(160.dp)
    ) {
        // Column header
        Text(
            text = header,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Preview rows
        previewValues.forEach { value ->
            Text(
                text = value.ifBlank { "-" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Field mapping dropdown
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedField,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .width(160.dp)
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                fields.forEach { field ->
                    DropdownMenuItem(
                        text = { Text(field, style = MaterialTheme.typography.bodySmall) },
                        onClick = {
                            onFieldSelected(field)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
