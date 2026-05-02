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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.model.CsvImportType

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

        val orderedIndices = remember(uiState.marketIndicesOrder) {
            val orderMap = uiState.marketIndicesOrder.withIndex().associate { it.value to it.index }
            SettingsViewModel.AVAILABLE_MARKET_INDICES.sortedBy { orderMap[it.symbol] ?: Int.MAX_VALUE }
        }

        orderedIndices.forEachIndexed { displayIndex, index ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Column {
                        IconButton(
                            onClick = { viewModel.moveMarketIndex(index.symbol, -1) },
                            enabled = displayIndex > 0,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move up",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.moveMarketIndex(index.symbol, 1) },
                            enabled = displayIndex < orderedIndices.lastIndex,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move down",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${index.label} (${index.symbol})",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
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

    var mappingPickerType by remember { mutableStateOf<CsvImportType?>(null) }
    var importPickerType by remember { mutableStateOf<CsvImportType?>(null) }
    var importAccountId by remember { mutableStateOf(-1L) }
    var importAccountExpanded by remember { mutableStateOf(false) }
    var showPositionImportWarning by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val csvMimeTypes = arrayOf("text/csv", "text/comma-separated-values", "*/*")

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

    val mappingCsvPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { fileUri ->
            mappingPickerType?.let { type ->
                viewModel.openMappingDialog(type, fileUri)
                mappingPickerType = null
            }
        }
    }

    val importCsvPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { fileUri ->
            importPickerType?.let { type ->
                if (type == CsvImportType.Position) {
                    pendingImportUri = fileUri
                    showPositionImportWarning = true
                } else {
                    viewModel.startCsvImport(type, fileUri, importAccountId)
                }
                importPickerType = null
            }
        }
    }

    LaunchedEffect(uiState.accounts) {
        if (importAccountId == -1L && uiState.accounts.isNotEmpty()) {
            importAccountId = uiState.accounts.first().id
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

    if (showPositionImportWarning) {
        AlertDialog(
            onDismissRequest = {
                showPositionImportWarning = false
                pendingImportUri = null
            },
            title = { Text("Import Positions") },
            text = {
                Text("Position details will be refreshed with imported CSV file. Are you sure?")
            },
            confirmButton = {
                TextButton(onClick = {
                    showPositionImportWarning = false
                    pendingImportUri?.let { viewModel.startCsvImport(CsvImportType.Position, it, importAccountId) }
                    pendingImportUri = null
                }) {
                    Text("Import", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPositionImportWarning = false
                    pendingImportUri = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Import progress dialog
    uiState.csvImport?.let { csvState ->
        if (csvState.isImporting) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Importing ${csvState.importType.label}...") },
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
        }
    }

    // Mapping dialog
    uiState.csvMappingDialog?.let { dialog ->
        CsvMappingDialog(
            dialog = dialog,
            onFieldChanged = { colIndex, field -> viewModel.updateMappingDialogField(colIndex, field) },
            onDateFormatChanged = { colIndex, fmt -> viewModel.updateMappingDialogDateFormat(colIndex, fmt) },
            onSave = { viewModel.saveMappingDialog() },
            onDismiss = { viewModel.dismissMappingDialog() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // --- Import Data ---
        Text("Import Data (CSV)", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Define column mappings first, then import CSV files.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Account selector for imports
        Text("Target Account", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(
            expanded = importAccountExpanded,
            onExpandedChange = { importAccountExpanded = it }
        ) {
            OutlinedTextField(
                value = uiState.accounts.find { it.id == importAccountId }?.name ?: "Select account",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = importAccountExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = importAccountExpanded,
                onDismissRequest = { importAccountExpanded = false }
            ) {
                uiState.accounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text(account.name) },
                        onClick = {
                            importAccountId = account.id
                            importAccountExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        CsvImportType.entries.forEach { type ->
            ImportTypeCard(
                type = type,
                enabled = importAccountId != -1L,
                onDefineMapping = {
                    mappingPickerType = type
                    mappingCsvPicker.launch(csvMimeTypes)
                },
                onStartImport = {
                    importPickerType = type
                    importCsvPicker.launch(csvMimeTypes)
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(24.dp))

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

        Spacer(modifier = Modifier.height(24.dp))

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

        Spacer(modifier = Modifier.height(24.dp))

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

@Composable
private fun ImportTypeCard(
    type: CsvImportType,
    enabled: Boolean,
    onDefineMapping: () -> Unit,
    onStartImport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = type.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Required: ${type.requiredFields.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDefineMapping,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Define Mapping")
                }
                Button(
                    onClick = onStartImport,
                    modifier = Modifier.weight(1f),
                    enabled = enabled
                ) {
                    Text("Start Import")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CsvMappingDialog(
    dialog: CsvMappingDialogState,
    onFieldChanged: (Int, String) -> Unit,
    onDateFormatChanged: (Int, String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val fields = dialog.importType.mappableFields
    val dateTimeFields = dialog.importType.dateTimeFields

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Map Columns — ${dialog.importType.label}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                if (dialog.hasSavedMapping && dialog.csvHeaders.isEmpty()) {
                    Text(
                        "Existing mapping loaded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (!dialog.hasSavedMapping && dialog.csvHeaders.isEmpty()) {
                    Text(
                        "No existing mapping.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (dialog.csvHeaders.isNotEmpty()) {
                    // Header row
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "CSV Column",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "Mapped Field",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "Options",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    dialog.csvHeaders.forEachIndexed { colIndex, header ->
                        val selectedField = dialog.columnMappings[colIndex] ?: "Skip"
                        val isDateField = selectedField in dateTimeFields

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // CSV Column name + preview
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = header,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                dialog.previewRows.firstOrNull()?.getOrNull(colIndex)?.let {
                                    Text(
                                        text = it.ifBlank { "-" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Mapped field dropdown
                            CsvFieldDropdown(
                                selected = selectedField,
                                fields = fields,
                                onSelected = { onFieldChanged(colIndex, it) },
                                modifier = Modifier.weight(1f)
                            )

                            // Options column (date format for date/time fields)
                            Column(modifier = Modifier.weight(1f)) {
                                if (isDateField) {
                                    OutlinedTextField(
                                        value = dialog.dateFormats[colIndex] ?: "",
                                        onValueChange = { onDateFormatChanged(colIndex, it) },
                                        placeholder = { Text("MM/dd/yyyy", style = MaterialTheme.typography.labelSmall) },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text("Update Mapping")
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
private fun CsvFieldDropdown(
    selected: String,
    fields: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            singleLine = true,
            textStyle = MaterialTheme.typography.labelSmall
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            fields.forEach { field ->
                DropdownMenuItem(
                    text = { Text(field, style = MaterialTheme.typography.bodySmall) },
                    onClick = {
                        onSelected(field)
                        expanded = false
                    }
                )
            }
        }
    }
}
