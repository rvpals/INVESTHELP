package com.investhelp.app.ui.performance

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import com.investhelp.app.ui.components.CollapsibleCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.data.local.entity.AccountPerformanceEntity
import com.investhelp.app.ui.settings.SettingsViewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccountPerformanceScreen(
    viewModel: AccountPerformanceViewModel
) {
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle()
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val chartData by viewModel.chartData.collectAsStateWithLifecycle()
    val pulledValue by viewModel.pulledValue.collectAsStateWithLifecycle()
    val pinStates by viewModel.pinStates.collectAsStateWithLifecycle()
    val saveError by viewModel.saveError.collectAsStateWithLifecycle()

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    val dateInputFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")

    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var totalValueText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(LocalDate.now().format(dateInputFormatter)) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<AccountPerformanceEntity?>(null) }
    var editTarget by remember { mutableStateOf<AccountPerformanceEntity?>(null) }
    var editNoteText by remember { mutableStateOf("") }
    var editDateTarget by remember { mutableStateOf<AccountPerformanceEntity?>(null) }
    var editDateText by remember { mutableStateOf("") }
    var chartSelectedAccountIds by remember { mutableStateOf(setOf<Long>()) }
    var smoothCurve by remember { mutableStateOf(false) }
    var showFullScreenChart by remember { mutableStateOf(false) }

    // Records filter & sort state (persisted via ViewModel)
    val recordFilterAccountIds by viewModel.recordFilterAccountIds.collectAsStateWithLifecycle()
    var recordFilterExpanded by remember { mutableStateOf(false) }
    val recordSortField by viewModel.recordSortField.collectAsStateWithLifecycle()
    var recordSortExpanded by remember { mutableStateOf(false) }
    val recordSortAsc by viewModel.recordSortAsc.collectAsStateWithLifecycle()
    var recordSortDirExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val warnBeforeDelete = remember {
        context.getSharedPreferences(SettingsViewModel.PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(SettingsViewModel.KEY_WARN_BEFORE_DELETE, true)
    }

    // Auto-select first account
    LaunchedEffect(accounts) {
        if (selectedAccountId == null && accounts.isNotEmpty()) {
            selectedAccountId = accounts.first().id
        }
        if (chartSelectedAccountIds.isEmpty() && accounts.isNotEmpty()) {
            chartSelectedAccountIds = setOf(accounts.first().id)
        }
        if (recordFilterAccountIds == null && accounts.isNotEmpty()) {
            viewModel.setRecordFilterAccountIds(accounts.map { it.id }.toSet())
        }
    }

    // Load chart data when selected accounts change
    LaunchedEffect(chartSelectedAccountIds) {
        viewModel.loadChartData(chartSelectedAccountIds)
    }

    // Apply pulled value
    LaunchedEffect(pulledValue) {
        pulledValue?.let {
            totalValueText = "%.2f".format(it)
            viewModel.clearPulledValue()
        }
    }

    // Delete confirmation dialog
    deleteTarget?.let { record ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Record") },
            text = { Text("Delete this performance record?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRecord(record)
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Edit note dialog
    editTarget?.let { record ->
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("Edit Note") },
            text = {
                OutlinedTextField(
                    value = editNoteText,
                    onValueChange = { editNoteText = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateRecord(record.copy(note = editNoteText.trim()))
                    editTarget = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Edit date dialog
    editDateTarget?.let { record ->
        val parsedDate = try {
            LocalDate.parse(editDateText, dateInputFormatter)
        } catch (_: Exception) { null }
        AlertDialog(
            onDismissRequest = { editDateTarget = null },
            title = { Text("Edit Date") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = editDateText,
                            onValueChange = { editDateText = it },
                            label = { Text("Date (MM/dd/yyyy)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            isError = parsedDate == null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(onClick = {
                            editDateText = LocalDate.now().format(dateInputFormatter)
                        }) { Text("Today") }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        parsedDate?.let {
                            viewModel.updateRecord(record.copy(date = it))
                            editDateTarget = null
                        }
                    },
                    enabled = parsedDate != null
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editDateTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Duplicate record error dialog
    saveError?.let { errorMsg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearSaveError() },
            title = { Text("Cannot Add Record") },
            text = { Text(errorMsg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearSaveError() }) { Text("OK") }
            }
        )
    }

    // Full-screen chart dialog
    if (showFullScreenChart) {
        val fullSeriesList = remember(chartData, accounts) {
            chartData.entries
                .filter { (_, records) -> records.size >= 2 }
                .map { (accountId, records) ->
                    val accountIndex = accounts.indexOfFirst { it.id == accountId }
                    val color = CHART_COLORS[
                        (if (accountIndex >= 0) accountIndex else 0) % CHART_COLORS.size
                    ]
                    ChartSeries(
                        accountId = accountId,
                        accountName = accounts.find { it.id == accountId }?.name ?: "Unknown",
                        color = color,
                        records = records
                    )
                }
        }
        Dialog(
            onDismissRequest = { showFullScreenChart = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Performance Chart") },
                        navigationIcon = {
                            IconButton(onClick = { showFullScreenChart = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 8.dp)
                ) {
                    // Legend
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        fullSeriesList.forEach { series ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Canvas(modifier = Modifier.size(10.dp)) {
                                    drawCircle(color = series.color)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(series.accountName, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Checkbox(checked = smoothCurve, onCheckedChange = { smoothCurve = it })
                        Text("Smooth Curve", style = MaterialTheme.typography.bodySmall)
                    }

                    if (fullSeriesList.isNotEmpty()) {
                        PerformanceLineChart(
                            seriesList = fullSeriesList,
                            smoothCurve = smoothCurve,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 8.dp)
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Account Performance") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Add Performance Record section
            item {
                CollapsibleCard(
                    title = "Add Performance Record",
                    pinned = pinStates[AccountPerformanceViewModel.KEY_PIN_ADD_RECORD] == true,
                    onPinToggle = { viewModel.setPinState(AccountPerformanceViewModel.KEY_PIN_ADD_RECORD, it) }
                ) {
                    Column {
                        // Account selector
                        ExposedDropdownMenuBox(
                            expanded = accountDropdownExpanded,
                            onExpandedChange = { accountDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = accounts.find { it.id == selectedAccountId }?.name ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Account") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = accountDropdownExpanded,
                                onDismissRequest = { accountDropdownExpanded = false }
                            ) {
                                accounts.forEach { account ->
                                    DropdownMenuItem(
                                        text = { Text(account.name) },
                                        onClick = {
                                            selectedAccountId = account.id
                                            accountDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Total Value + Pull from App
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = totalValueText,
                                onValueChange = { totalValueText = it },
                                label = { Text("Total Value") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.pullValueFromApp() }
                            ) {
                                Text("Pull from App")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Date field + Today button
                        val addParsedDate = try {
                            LocalDate.parse(dateText, dateInputFormatter)
                        } catch (_: Exception) { null }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = dateText,
                                onValueChange = { dateText = it },
                                label = { Text("Date") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                isError = addParsedDate == null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(onClick = {
                                dateText = LocalDate.now().format(dateInputFormatter)
                            }) { Text("Today") }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Note field
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            label = { Text("Note (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val value = totalValueText.toDoubleOrNull()
                                val parsedDt = try {
                                    LocalDate.parse(dateText, dateInputFormatter)
                                } catch (_: Exception) { null }
                                if (selectedAccountId != null && value != null && parsedDt != null) {
                                    viewModel.saveRecord(selectedAccountId!!, value, noteText, parsedDt)
                                    totalValueText = ""
                                    noteText = ""
                                    dateText = LocalDate.now().format(dateInputFormatter)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedAccountId != null && totalValueText.toDoubleOrNull() != null && addParsedDate != null
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Record")
                        }
                    }
                }
            }

            // Performance Charts section
            item {
                CollapsibleCard(
                    title = "Performance Charts",
                    pinned = pinStates[AccountPerformanceViewModel.KEY_PIN_CHART] == true,
                    onPinToggle = { viewModel.setPinState(AccountPerformanceViewModel.KEY_PIN_CHART, it) }
                ) {
                    Column {
                        // Multi-account selector
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            accounts.forEachIndexed { index, account ->
                                val color = CHART_COLORS[index % CHART_COLORS.size]
                                val selected = account.id in chartSelectedAccountIds
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        chartSelectedAccountIds = if (selected) {
                                            chartSelectedAccountIds - account.id
                                        } else {
                                            chartSelectedAccountIds + account.id
                                        }
                                    },
                                    label = { Text(account.name) },
                                    leadingIcon = if (selected) {
                                        {
                                            Canvas(modifier = Modifier.size(8.dp)) {
                                                drawCircle(color = color)
                                            }
                                        }
                                    } else null
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Checkbox(
                                checked = smoothCurve,
                                onCheckedChange = { smoothCurve = it }
                            )
                            Text("Smooth Curve", style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val seriesList = remember(chartData, accounts) {
                            chartData.entries
                                .filter { (_, records) -> records.size >= 2 }
                                .map { (accountId, records) ->
                                    val accountIndex = accounts.indexOfFirst { it.id == accountId }
                                    val color = CHART_COLORS[
                                        (if (accountIndex >= 0) accountIndex else 0) % CHART_COLORS.size
                                    ]
                                    ChartSeries(
                                        accountId = accountId,
                                        accountName = accounts.find { it.id == accountId }?.name ?: "Unknown",
                                        color = color,
                                        records = records
                                    )
                                }
                        }

                        if (seriesList.isNotEmpty()) {
                            PerformanceLineChart(
                                seriesList = seriesList,
                                smoothCurve = smoothCurve,
                                onDoubleTap = { showFullScreenChart = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                            )
                        } else if (chartSelectedAccountIds.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        "Need at least 2 records per account to show chart",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Records section
            item {
                val filterIds = recordFilterAccountIds ?: accounts.map { it.id }.toSet()
                val sortFields = listOf("Account", "Date", "Total Value", "Note")

                val filteredAndSorted = remember(allRecords, filterIds, recordSortField, recordSortAsc, accounts) {
                    val filtered = allRecords.filter { it.accountId in filterIds }
                    val comparator: Comparator<AccountPerformanceEntity> = when (recordSortField) {
                        "Account" -> compareBy { accounts.find { a -> a.id == it.accountId }?.name ?: "" }
                        "Total Value" -> compareBy { it.totalValue }
                        "Note" -> compareBy { it.note }
                        else -> compareBy { it.date }
                    }
                    if (recordSortAsc) filtered.sortedWith(comparator) else filtered.sortedWith(comparator.reversed())
                }

                CollapsibleCard(
                    title = "Records (${filteredAndSorted.size})",
                    pinned = pinStates[AccountPerformanceViewModel.KEY_PIN_RECORDS] == true,
                    onPinToggle = { viewModel.setPinState(AccountPerformanceViewModel.KEY_PIN_RECORDS, it) }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Filter & Sort controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Account filter dropdown
                            Column(modifier = Modifier.weight(1f)) {
                                val selectedCount = filterIds.size
                                val totalCount = accounts.size
                                val filterLabel = when (selectedCount) {
                                    0 -> "None"
                                    totalCount -> "All Accounts"
                                    1 -> accounts.find { it.id in filterIds }?.name ?: "1 Account"
                                    else -> "$selectedCount Accounts"
                                }
                                OutlinedButton(
                                    onClick = { recordFilterExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(filterLabel, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                                DropdownMenu(
                                    expanded = recordFilterExpanded,
                                    onDismissRequest = { recordFilterExpanded = false }
                                ) {
                                    // Select All / None
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Select All",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        onClick = { viewModel.setRecordFilterAccountIds(accounts.map { it.id }.toSet()) }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Select None",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        onClick = { viewModel.setRecordFilterAccountIds(emptySet()) }
                                    )
                                    HorizontalDivider()
                                    accounts.forEach { account ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Checkbox(
                                                        checked = account.id in filterIds,
                                                        onCheckedChange = {
                                                            viewModel.setRecordFilterAccountIds(
                                                                if (account.id in filterIds) filterIds - account.id
                                                                else filterIds + account.id
                                                            )
                                                        }
                                                    )
                                                    Text(account.name, style = MaterialTheme.typography.bodySmall)
                                                }
                                            },
                                            onClick = {
                                                viewModel.setRecordFilterAccountIds(
                                                    if (account.id in filterIds) filterIds - account.id
                                                    else filterIds + account.id
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            // Order By dropdown
                            Column {
                                OutlinedButton(onClick = { recordSortExpanded = true }) {
                                    Text(recordSortField, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                                DropdownMenu(
                                    expanded = recordSortExpanded,
                                    onDismissRequest = { recordSortExpanded = false }
                                ) {
                                    sortFields.forEach { field ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    field,
                                                    fontWeight = if (field == recordSortField) FontWeight.Bold else FontWeight.Normal,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            },
                                            onClick = {
                                                viewModel.setRecordSortField(field)
                                                recordSortExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Asc/Desc dropdown
                            Column {
                                OutlinedButton(onClick = { recordSortDirExpanded = true }) {
                                    Text(
                                        if (recordSortAsc) "Asc" else "Desc",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                DropdownMenu(
                                    expanded = recordSortDirExpanded,
                                    onDismissRequest = { recordSortDirExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Asc",
                                                fontWeight = if (recordSortAsc) FontWeight.Bold else FontWeight.Normal,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        },
                                        onClick = { viewModel.setRecordSortAsc(true); recordSortDirExpanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Desc",
                                                fontWeight = if (!recordSortAsc) FontWeight.Bold else FontWeight.Normal,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        },
                                        onClick = { viewModel.setRecordSortAsc(false); recordSortDirExpanded = false }
                                    )
                                }
                            }
                        }

                        // Records list
                        if (filteredAndSorted.isEmpty()) {
                            Text(
                                if (allRecords.isEmpty()) "No performance records yet" else "No records match filter",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        filteredAndSorted.forEach { record ->
                            val accountName = accounts.find { it.id == record.accountId }?.name ?: "Unknown"
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = accountName,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = record.date.format(dateFormatter),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.clickable {
                                                editDateText = record.date.format(dateInputFormatter)
                                                editDateTarget = record
                                            }
                                        )
                                        if (record.note.isNotBlank()) {
                                            Text(
                                                text = record.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = currencyFormat.format(record.totalValue),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    IconButton(onClick = {
                                        editNoteText = record.note
                                        editTarget = record
                                    }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit note",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = {
                                        if (warnBeforeDelete) {
                                            deleteTarget = record
                                        } else {
                                            viewModel.deleteRecord(record)
                                        }
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

private val CHART_COLORS = listOf(
    Color(0xFF4285F4), // Blue
    Color(0xFFEA4335), // Red
    Color(0xFF34A853), // Green
    Color(0xFFFBBC04), // Amber
    Color(0xFF9C27B0), // Purple
    Color(0xFFFF6D00), // Orange
    Color(0xFF00ACC1), // Cyan
    Color(0xFF795548), // Brown
)

private data class ChartSeries(
    val accountId: Long,
    val accountName: String,
    val color: Color,
    val records: List<AccountPerformanceEntity>
)

@Composable
private fun PerformanceLineChart(
    seriesList: List<ChartSeries>,
    smoothCurve: Boolean = false,
    onDoubleTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (seriesList.isEmpty()) return

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tooltipBg = MaterialTheme.colorScheme.inverseSurface
    val tooltipTextColor = MaterialTheme.colorScheme.inverseOnSurface

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")

    // Global value range across all series
    val allValues = seriesList.flatMap { s -> s.records.map { it.totalValue } }
    val globalMin = allValues.min() * 0.998
    val globalMax = allValues.max() * 1.002
    val valRange = (globalMax - globalMin).let { if (it < 0.01) 1.0 else it }

    // Global time range across all series
    val allEpochs = seriesList.flatMap { s -> s.records.map { it.date.toEpochDay() } }
    val minEpoch = allEpochs.min()
    val maxEpoch = allEpochs.max()
    val timeRange = (maxEpoch - minEpoch).let { if (it < 1L) 1L else it }

    // Zoom and pan state
    var zoom by remember(seriesList) { mutableStateOf(1f) }
    var scrollOffset by remember(seriesList) { mutableStateOf(0f) }
    var chartWidthPx by remember { mutableStateOf(1f) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val oldZoom = zoom
        val newZoom = (oldZoom * zoomChange).coerceIn(1f, 5f)
        val maxScroll = (chartWidthPx * newZoom - chartWidthPx).coerceAtLeast(0f)
        val newScroll = (scrollOffset + chartWidthPx / 2) * (newZoom / oldZoom) -
                chartWidthPx / 2 - panChange.x
        scrollOffset = newScroll.coerceIn(0f, maxScroll)
        zoom = newZoom
    }

    // Selection state
    var selectedSeriesIdx by remember(seriesList) { mutableStateOf<Int?>(null) }
    var selectedPointIdx by remember(seriesList) { mutableStateOf<Int?>(null) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                seriesList.forEach { series ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(10.dp)) {
                            drawCircle(color = series.color)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            series.accountName,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 24.dp, start = 48.dp, end = 12.dp)
                    .transformable(transformState)
                    .pointerInput(seriesList) {
                        detectTapGestures(
                            onTap = { offset ->
                                val cw = size.width.toFloat()
                                val virtualWidth = cw * zoom
                                var bestDist = Float.MAX_VALUE
                                var bestSeries = -1
                                var bestPoint = -1

                                seriesList.forEachIndexed { sIdx, series ->
                                    series.records.forEachIndexed { pIdx, record ->
                                        val epoch = record.date.toEpochDay()
                                        val normX = (epoch - minEpoch).toFloat() / timeRange
                                        val screenX = normX * virtualWidth - scrollOffset
                                        val dist = abs(offset.x - screenX)
                                        if (dist < bestDist) {
                                            bestDist = dist
                                            bestSeries = sIdx
                                            bestPoint = pIdx
                                        }
                                    }
                                }

                                if (bestDist < 30.dp.toPx()) {
                                    if (selectedSeriesIdx == bestSeries && selectedPointIdx == bestPoint) {
                                        selectedSeriesIdx = null
                                        selectedPointIdx = null
                                    } else {
                                        selectedSeriesIdx = bestSeries
                                        selectedPointIdx = bestPoint
                                    }
                                } else {
                                    selectedSeriesIdx = null
                                    selectedPointIdx = null
                                }
                            },
                            onDoubleTap = {
                                if (onDoubleTap != null) {
                                    onDoubleTap()
                                } else {
                                    zoom = 1f
                                    scrollOffset = 0f
                                    selectedSeriesIdx = null
                                    selectedPointIdx = null
                                }
                            }
                        )
                    }
            ) {
                val chartWidth = size.width
                val chartHeight = size.height
                chartWidthPx = chartWidth
                val virtualWidth = chartWidth * zoom

                // Grid lines + Y-axis labels
                for (i in 0..3) {
                    val y = chartHeight * i / 3f
                    drawLine(gridColor, Offset(0f, y), Offset(chartWidth, y), 1f)
                    val price = globalMax - (valRange * i / 3)
                    drawContext.canvas.nativeCanvas.drawText(
                        currencyFormat.format(price),
                        -44.dp.toPx(),
                        y + 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = labelColor.hashCode()
                            textSize = 9.dp.toPx()
                            textAlign = android.graphics.Paint.Align.LEFT
                        }
                    )
                }

                // Clip data area for zoomed content
                clipRect(0f, 0f, chartWidth, chartHeight) {
                    // Draw each series
                    for (series in seriesList) {
                        if (series.records.size < 2) continue
                        val seriesColor = series.color

                        val path = Path()
                        val fillPath = Path()
                        var lastScreenX = 0f

                        val points = series.records.map { record ->
                            val epoch = record.date.toEpochDay()
                            val normX = (epoch - minEpoch).toFloat() / timeRange
                            val screenX = normX * virtualWidth - scrollOffset
                            val screenY = ((globalMax - record.totalValue) / valRange * chartHeight).toFloat()
                            Offset(screenX, screenY)
                        }

                        points.forEachIndexed { i, pt ->
                            lastScreenX = pt.x
                            if (i == 0) {
                                path.moveTo(pt.x, pt.y)
                                fillPath.moveTo(pt.x, chartHeight)
                                fillPath.lineTo(pt.x, pt.y)
                            } else if (smoothCurve) {
                                val prev = points[i - 1]
                                val cpX = (prev.x + pt.x) / 2f
                                path.cubicTo(cpX, prev.y, cpX, pt.y, pt.x, pt.y)
                                fillPath.cubicTo(cpX, prev.y, cpX, pt.y, pt.x, pt.y)
                            } else {
                                path.lineTo(pt.x, pt.y)
                                fillPath.lineTo(pt.x, pt.y)
                            }
                        }

                        fillPath.lineTo(lastScreenX, chartHeight)
                        fillPath.close()

                        val fillAlpha = if (seriesList.size == 1) 0.1f else 0.05f
                        drawPath(fillPath, color = seriesColor.copy(alpha = fillAlpha))
                        drawPath(
                            path = path,
                            color = seriesColor,
                            style = Stroke(width = 3f, cap = StrokeCap.Round)
                        )

                        // Data point dots — bold for records with notes
                        series.records.forEach { record ->
                            val epoch = record.date.toEpochDay()
                            val normX = (epoch - minEpoch).toFloat() / timeRange
                            val screenX = normX * virtualWidth - scrollOffset
                            val screenY = ((globalMax - record.totalValue) / valRange * chartHeight).toFloat()
                            val hasNote = record.note.isNotBlank()
                            if (hasNote) {
                                drawCircle(color = Color.White, radius = 9f, center = Offset(screenX, screenY))
                                drawCircle(color = seriesColor, radius = 7f, center = Offset(screenX, screenY))
                            } else {
                                drawCircle(color = seriesColor, radius = 4f, center = Offset(screenX, screenY))
                            }
                        }
                    }

                    // Tooltip for selected point
                    if (selectedSeriesIdx != null && selectedPointIdx != null) {
                        val sIdx = selectedSeriesIdx!!
                        val pIdx = selectedPointIdx!!
                        if (sIdx < seriesList.size && pIdx < seriesList[sIdx].records.size) {
                            val series = seriesList[sIdx]
                            val record = series.records[pIdx]
                            val epoch = record.date.toEpochDay()
                            val normX = (epoch - minEpoch).toFloat() / timeRange
                            val screenX = normX * virtualWidth - scrollOffset
                            val screenY = ((globalMax - record.totalValue) / valRange * chartHeight).toFloat()

                            // Vertical dashed line
                            drawLine(
                                color = labelColor.copy(alpha = 0.4f),
                                start = Offset(screenX, 0f),
                                end = Offset(screenX, chartHeight),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                            )

                            // Point highlight
                            drawCircle(color = Color.White, radius = 7f, center = Offset(screenX, screenY))
                            drawCircle(color = series.color, radius = 5f, center = Offset(screenX, screenY))

                            // Tooltip box
                            val tooltipDateFmt = DateTimeFormatter.ofPattern("MM/dd/yy")
                            val line1 = "${series.accountName}: ${currencyFormat.format(record.totalValue)}  ${record.date.format(tooltipDateFmt)}"
                            val hasNote = record.note.isNotBlank()
                            val paint = android.graphics.Paint().apply {
                                textSize = 11.dp.toPx()
                                isAntiAlias = true
                            }
                            val line1Width = paint.measureText(line1)
                            val tooltipPadH = 8.dp.toPx()
                            val tooltipPadV = 5.dp.toPx()
                            val lineHeight = paint.textSize * 1.3f

                            val noteText = if (hasNote) record.note else null
                            val noteWidth = noteText?.let { paint.measureText(it) } ?: 0f
                            val tooltipW = maxOf(line1Width, noteWidth) + tooltipPadH * 2
                            val tooltipH = if (hasNote) {
                                tooltipPadV * 2 + lineHeight + paint.textSize
                            } else {
                                paint.textSize + tooltipPadV * 2
                            }
                            val tooltipX = (screenX - tooltipW / 2).coerceIn(0f, chartWidth - tooltipW)
                            val tooltipY = (screenY - tooltipH - 12.dp.toPx()).coerceAtLeast(0f)

                            drawRoundRect(
                                color = tooltipBg,
                                topLeft = Offset(tooltipX, tooltipY),
                                size = Size(tooltipW, tooltipH),
                                cornerRadius = CornerRadius(6.dp.toPx())
                            )
                            drawContext.canvas.nativeCanvas.drawText(
                                line1,
                                tooltipX + tooltipPadH,
                                tooltipY + tooltipPadV + paint.textSize * 0.85f,
                                paint.apply { color = tooltipTextColor.hashCode() }
                            )
                            if (hasNote && noteText != null) {
                                val notePaint = android.graphics.Paint().apply {
                                    textSize = 11.dp.toPx()
                                    isAntiAlias = true
                                    color = tooltipTextColor.hashCode()
                                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                                }
                                drawContext.canvas.nativeCanvas.drawText(
                                    noteText,
                                    tooltipX + tooltipPadH,
                                    tooltipY + tooltipPadV + lineHeight + paint.textSize * 0.85f,
                                    notePaint
                                )
                            }
                        }
                    }
                }

                // X-axis date labels (viewport-aware)
                val leftFraction = (scrollOffset / virtualWidth).coerceIn(0f, 1f)
                val centerFraction = ((scrollOffset + chartWidth / 2) / virtualWidth).coerceIn(0f, 1f)
                val rightFraction = ((scrollOffset + chartWidth) / virtualWidth).coerceIn(0f, 1f)

                listOf(
                    0f to leftFraction,
                    chartWidth / 2 to centerFraction,
                    chartWidth to rightFraction
                ).forEach { (screenX, fraction) ->
                    val epochDay = minEpoch + (fraction * timeRange).toLong()
                    val labelDate = LocalDate.ofEpochDay(epochDay)
                    drawContext.canvas.nativeCanvas.drawText(
                        labelDate.format(dateFormatter),
                        screenX,
                        chartHeight + 16.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = labelColor.hashCode()
                            textSize = 9.dp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}
