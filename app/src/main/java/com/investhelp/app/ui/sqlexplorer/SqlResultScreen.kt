package com.investhelp.app.ui.sqlexplorer

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.ui.components.CollapsibleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SqlResultScreen(
    initialSql: String,
    viewModel: SqlExplorerViewModel,
    onBack: () -> Unit
) {
    val result by viewModel.result.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var sql by rememberSaveable { mutableStateOf(initialSql) }
    var hasRun by rememberSaveable { mutableStateOf(false) }
    var showCellDetail by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(Unit) {
        if (!hasRun && sql.isNotBlank()) {
            viewModel.executeQuery(sql)
            hasRun = true
        }
    }

    if (showCellDetail != null) {
        CellDetailDialog(
            columnName = showCellDetail!!.first,
            value = showCellDetail!!.second,
            onDismiss = { showCellDetail = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SQL Result") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                var queryPinned by rememberSaveable { mutableStateOf(true) }
                CollapsibleCard(
                    title = "SQL Query",
                    pinned = queryPinned,
                    onPinToggle = { queryPinned = it }
                ) {
                    OutlinedTextField(
                        value = sql,
                        onValueChange = { sql = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 10,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.executeQuery(sql) },
                        enabled = sql.isNotBlank() && !isRunning
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
                }
            }

            item {
                var resultPinned by rememberSaveable { mutableStateOf(true) }
                CollapsibleCard(
                    title = "Result",
                    pinned = resultPinned,
                    onPinToggle = { resultPinned = it }
                ) {
                    if (error != null) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    }

                    result?.let { qr ->
                        Text(
                            text = if (qr.message != null) qr.message
                            else "${qr.rowCount} row${if (qr.rowCount != 1) "s" else ""} in ${qr.executionTimeMs}ms",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (qr.columns.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ResultGrid(
                                result = qr,
                                onCellClick = { col, value -> showCellDetail = col to value }
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.exportCsv()?.let { intent ->
                                        context.startActivity(Intent.createChooser(intent, "Export CSV"))
                                    }
                                }
                            ) {
                                Text("Export to CSV file")
                            }
                        }
                    }

                    if (result == null && error == null && !isRunning) {
                        Text(
                            "Run a query to see results",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ResultGrid(
    result: QueryResult,
    onCellClick: (String, String) -> Unit
) {
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()
    val columnWidth = 120.dp
    val dividerColor = MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .verticalScroll(verticalScroll)
                .horizontalScroll(horizontalScroll)
                .padding(8.dp)
        ) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                result.columns.forEachIndexed { i, col ->
                    if (i > 0) VerticalDivider(thickness = 1.dp, color = dividerColor)
                    Text(
                        text = col,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.width(columnWidth).padding(4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            HorizontalDivider(thickness = 2.dp, color = dividerColor)

            result.rows.forEachIndexed { index, row ->
                val rowBg = if (index % 2 == 1)
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                else
                    Color.Transparent
                Row(
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                        .background(rowBg)
                ) {
                    row.forEachIndexed { i, cell ->
                        if (i > 0) VerticalDivider(thickness = 1.dp, color = dividerColor)
                        Text(
                            text = cell,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier
                                .width(columnWidth)
                                .padding(4.dp)
                                .clickable { onCellClick(result.columns[i], cell) },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                HorizontalDivider(color = dividerColor)
            }
        }
    }
}

@Composable
private fun CellDetailDialog(
    columnName: String,
    value: String,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.material3.Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                androidx.compose.material3.TopAppBar(
                    title = { Text(columnName) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = columnName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                )
            }
        }
    }
}
