package com.investhelp.app.ui.sqlexplorer

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SqlExplorerScreen(viewModel: SqlExplorerViewModel) {
    val result by viewModel.result.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val tables by viewModel.tables.collectAsStateWithLifecycle()
    val expandedTable by viewModel.expandedTable.collectAsStateWithLifecycle()
    val tableColumns by viewModel.tableColumns.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var sql by rememberSaveable { mutableStateOf("") }
    var selectedRowIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = sql,
            onValueChange = { sql = it },
            label = { Text("SQL Query") },
            placeholder = { Text("SELECT * FROM investment_items LIMIT 10") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.executeQuery(sql) },
                enabled = sql.isNotBlank() && !isRunning,
                modifier = Modifier.weight(1f)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp).width(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                }
                Text("Run")
            }
            OutlinedButton(
                onClick = {
                    viewModel.exportCsv()?.let { intent ->
                        context.startActivity(Intent.createChooser(intent, "Export CSV"))
                    }
                },
                enabled = result?.columns?.isNotEmpty() == true
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("Export CSV")
            }
        }

        if (tables.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            TableBrowser(
                tables = tables,
                expandedTable = expandedTable,
                tableColumns = tableColumns,
                onTableClick = viewModel::toggleTable,
                onOpenTable = { tableName ->
                    sql = "SELECT * FROM $tableName"
                    viewModel.executeQuery(sql)
                }
            )
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            )
        }

        result?.let { qr ->
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (qr.message != null) qr.message
                else "${qr.rowCount} row${if (qr.rowCount != 1) "s" else ""} in ${qr.executionTimeMs}ms",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (qr.columns.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                ResultTable(qr) { index -> selectedRowIndex = index }
            }
        }

        if (selectedRowIndex != null && result != null &&
            selectedRowIndex!! < result!!.rows.size
        ) {
            RecordDetailDialog(
                columns = result!!.columns,
                row = result!!.rows[selectedRowIndex!!],
                rowNumber = selectedRowIndex!! + 1,
                onDismiss = { selectedRowIndex = null }
            )
        }
    }
}

@Composable
private fun ResultTable(result: QueryResult, onRowClick: (Int) -> Unit) {
    val horizontalScroll = rememberScrollState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScroll)
                .padding(8.dp)
        ) {
            // Header row
            item(key = "header") {
                Row {
                    result.columns.forEach { col ->
                        Text(
                            text = col,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            modifier = Modifier.width(120.dp).padding(4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                HorizontalDivider(thickness = 2.dp)
            }

            // Data rows
            itemsIndexed(result.rows, key = { index, _ -> index }) { index, row ->
                Row(modifier = Modifier.clickable { onRowClick(index) }) {
                    row.forEach { cell ->
                        Text(
                            text = cell,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.width(120.dp).padding(4.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun RecordDetailDialog(
    columns: List<String>,
    row: List<String>,
    rowNumber: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Row $rowNumber", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                columns.zip(row).forEach { (col, value) ->
                    Text(
                        text = col,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun TableBrowser(
    tables: List<String>,
    expandedTable: String?,
    tableColumns: List<ColumnInfo>,
    onTableClick: (String) -> Unit,
    onOpenTable: (String) -> Unit
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
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            fontWeight = if (expandedTable == tableName)
                                FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { onOpenTable(tableName) },
                            modifier = Modifier.height(30.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Open", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    AnimatedVisibility(visible = expandedTable == tableName) {
                        Column(modifier = Modifier.padding(start = 24.dp, bottom = 4.dp)) {
                            tableColumns.forEach { col ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = col.name,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        modifier = Modifier.width(140.dp)
                                    )
                                    Text(
                                        text = col.type.ifEmpty { "\u2014" },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(80.dp)
                                    )
                                    if (col.pk) {
                                        Text(
                                            text = "PK",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (col.notNull && !col.pk) {
                                        Text(
                                            text = "NN",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
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
