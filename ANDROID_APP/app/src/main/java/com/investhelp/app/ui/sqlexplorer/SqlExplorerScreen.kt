package com.investhelp.app.ui.sqlexplorer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.data.local.entity.SqlLibraryEntity
import com.investhelp.app.ui.components.CollapsibleCard
import com.investhelp.app.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SqlExplorerScreen(
    viewModel: SqlExplorerViewModel,
    onRunQuery: (String) -> Unit
) {
    val tables by viewModel.tables.collectAsStateWithLifecycle()
    val expandedTable by viewModel.expandedTable.collectAsStateWithLifecycle()
    val tableColumns by viewModel.tableColumns.collectAsStateWithLifecycle()
    val sqlLibrary by viewModel.sqlLibrary.collectAsStateWithLifecycle()
    val sqlCategories by viewModel.sqlCategories.collectAsStateWithLifecycle()

    var sql by rememberSaveable { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var tableToErase by remember { mutableStateOf<String?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val warnBeforeDelete = remember {
        context.getSharedPreferences(SettingsViewModel.PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getBoolean(SettingsViewModel.KEY_WARN_BEFORE_DELETE, true)
    }

    if (showSaveDialog) {
        SaveToLibraryDialog(
            categories = sqlCategories,
            onSave = { name, desc, category ->
                viewModel.saveToLibrary(name, desc, category, sql)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false }
        )
    }

    if (tableToErase != null) {
        AlertDialog(
            onDismissRequest = { tableToErase = null },
            title = { Text("Erase Table") },
            text = { Text("This will delete ALL entries from \"${tableToErase}\". This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eraseTable(tableToErase!!)
                    tableToErase = null
                }) { Text("Erase", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { tableToErase = null }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = sql,
                onValueChange = { sql = it },
                label = { Text("SQL Query") },
                placeholder = { Text("SELECT * FROM investment_positions LIMIT 10") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { if (sql.isNotBlank()) onRunQuery(sql) },
                    enabled = sql.isNotBlank()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Run")
                }
                OutlinedButton(
                    onClick = { showSaveDialog = true },
                    enabled = sql.isNotBlank()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Save SQL to Library")
                }
            }
        }

        item {
            var libraryPinned by rememberSaveable { mutableStateOf(false) }
            CollapsibleCard(
                title = "SQL Library",
                pinned = libraryPinned,
                onPinToggle = { libraryPinned = it }
            ) {
                SqlLibraryContent(
                    entries = sqlLibrary,
                    categories = sqlCategories,
                    onSelect = { entry ->
                        sql = entry.sql
                    },
                    onDelete = { entry ->
                        viewModel.deleteFromLibrary(entry)
                    },
                    onRun = { entry ->
                        onRunQuery(entry.sql)
                    }
                )
            }
        }

        item {
            TableBrowser(
                tables = tables,
                expandedTable = expandedTable,
                tableColumns = tableColumns,
                onTableClick = viewModel::toggleTable,
                onTableNameClick = { tableName ->
                    sql = sql + tableName
                },
                onColumnNameClick = { columnName ->
                    sql = sql + columnName
                },
                onOpenTable = { tableName ->
                    onRunQuery("SELECT * FROM $tableName")
                },
                onEraseTable = { tableName ->
                    if (warnBeforeDelete) {
                        tableToErase = tableName
                    } else {
                        viewModel.eraseTable(tableName)
                    }
                }
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SqlLibraryContent(
    entries: List<SqlLibraryEntity>,
    categories: List<String>,
    onSelect: (SqlLibraryEntity) -> Unit,
    onDelete: (SqlLibraryEntity) -> Unit,
    onRun: (SqlLibraryEntity) -> Unit
) {
    var filterCategory by rememberSaveable { mutableStateOf("") }
    var filterName by rememberSaveable { mutableStateOf("") }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = filterCategory.ifEmpty { "All" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All") },
                        onClick = { filterCategory = ""; categoryExpanded = false }
                    )
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = { filterCategory = cat; categoryExpanded = false }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = filterName,
                onValueChange = { filterName = it },
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val filtered = entries.filter { entry ->
            (filterCategory.isEmpty() || entry.category == filterCategory) &&
            (filterName.isEmpty() || entry.name.contains(filterName, ignoreCase = true))
        }

        if (filtered.isEmpty()) {
            Text(
                "No saved queries",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            val altColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            filtered.forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (index % 2 == 1) altColor else Color.Transparent)
                        .clickable { onSelect(entry) }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (entry.description.isNotBlank()) {
                            Text(
                                text = entry.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "[${entry.category}]",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = { onRun(entry) }) {
                        Text("Run", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(
                        onClick = { onDelete(entry) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun TableBrowser(
    tables: List<String>,
    expandedTable: String?,
    tableColumns: List<ColumnInfo>,
    onTableClick: (String) -> Unit,
    onTableNameClick: (String) -> Unit,
    onColumnNameClick: (String) -> Unit,
    onOpenTable: (String) -> Unit,
    onEraseTable: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Tables",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            tables.forEach { tableName ->
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTableClick(tableName) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (expandedTable == tableName)
                                Icons.Default.KeyboardArrowDown
                            else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tableName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = if (expandedTable == tableName) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onTableNameClick(tableName) }
                        )
                        TextButton(
                            onClick = { onOpenTable(tableName) },
                            modifier = Modifier.height(30.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Open", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(
                            onClick = { onEraseTable(tableName) },
                            modifier = Modifier.height(30.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Erase", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    AnimatedVisibility(visible = expandedTable == tableName) {
                        Column(modifier = Modifier.padding(start = 24.dp, bottom = 4.dp)) {
                            tableColumns.forEach { col ->
                                Row(
                                    modifier = Modifier
                                        .padding(vertical = 2.dp)
                                        .clickable { onColumnNameClick(col.name) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = col.name,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(140.dp)
                                    )
                                    Text(
                                        text = col.type.ifEmpty { "—" },
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(80.dp)
                                    )
                                    if (col.pk) {
                                        Text("PK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    if (col.notNull && !col.pk) {
                                        Text("NN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveToLibraryDialog(
    categories: List<String>,
    onSave: (name: String, description: String, category: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save SQL to Library") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                    )
                    if (categories.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = { category = cat; categoryExpanded = false }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), description.trim(), category.trim()) },
                enabled = name.isNotBlank() && category.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
