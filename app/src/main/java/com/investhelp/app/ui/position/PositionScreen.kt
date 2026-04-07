package com.investhelp.app.ui.position

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.data.local.entity.InvestmentAccountEntity
import com.investhelp.app.data.local.entity.PositionEntity
import com.investhelp.app.ui.components.ConfirmDeleteDialog
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PositionScreen(viewModel: PositionViewModel) {
    val positions by viewModel.positions.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showForm by remember { mutableStateOf(false) }
    var editingPosition by remember { mutableStateOf<PositionEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<PositionEntity?>(null) }

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val accountMap = accounts.associateBy { it.id }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (deleteTarget != null) {
        ConfirmDeleteDialog(
            title = "Delete Position",
            message = "Delete position for \"${deleteTarget!!.ticker}\"?",
            onConfirm = {
                viewModel.deletePosition(deleteTarget!!.ticker, deleteTarget!!.accountId)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }

    if (showForm || editingPosition != null) {
        PositionFormDialog(
            viewModel = viewModel,
            existing = editingPosition,
            onDismiss = {
                showForm = false
                editingPosition = null
            },
            onSave = { ticker, quantity, cost, accountId ->
                viewModel.savePosition(ticker, quantity, cost, accountId)
                showForm = false
                editingPosition = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Positions") },
                actions = {
                    TextButton(
                        onClick = { viewModel.refreshAllPositions() },
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text("Refresh All", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showForm = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Position")
            }
        }
    ) { padding ->
        if (positions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No positions yet", style = MaterialTheme.typography.bodyLarge)
                Text("Tap + to add one", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            // Aggregate values by ticker across all accounts
            val tickerValues = positions
                .groupBy { it.ticker }
                .mapValues { (_, list) -> list.sumOf { it.value } }
                .filter { it.value > 0 }
                .toList()
                .sortedByDescending { it.second }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (tickerValues.isNotEmpty()) {
                    item(key = "chart_section") {
                        ChartSection(tickerValues, currencyFormat)
                    }
                }

                items(positions, key = { "${it.ticker}:${it.accountId}" }) { position ->
                    PositionCard(
                        position = position,
                        accountName = accountMap[position.accountId]?.name,
                        currencyFormat = currencyFormat,
                        onEdit = { editingPosition = position },
                        onDelete = { deleteTarget = position }
                    )
                }
            }
        }
    }
}

@Composable
private fun PositionCard(
    position: PositionEntity,
    accountName: String?,
    currencyFormat: NumberFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val gainColor = if (position.totalGainLoss >= 0)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.error
    val dayColor = if (position.dayGainLoss >= 0)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(position.ticker, style = MaterialTheme.typography.titleMedium)
                    if (accountName != null) {
                        Text(
                            text = accountName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Qty: ${"%.4f".format(position.quantity)}  |  Cost: ${currencyFormat.format(position.cost)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Value", style = MaterialTheme.typography.labelSmall)
                        Text(
                            currencyFormat.format(position.value),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column {
                        Text("Day G/L", style = MaterialTheme.typography.labelSmall)
                        Text(
                            currencyFormat.format(position.dayGainLoss),
                            style = MaterialTheme.typography.bodyMedium,
                            color = dayColor
                        )
                    }
                    Column {
                        Text("Total G/L", style = MaterialTheme.typography.labelSmall)
                        Text(
                            currencyFormat.format(position.totalGainLoss),
                            style = MaterialTheme.typography.bodyMedium,
                            color = gainColor
                        )
                    }
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private val chartColors = listOf(
    Color(0xFF4285F4), // Blue
    Color(0xFFEA4335), // Red
    Color(0xFFFBBC04), // Yellow
    Color(0xFF34A853), // Green
    Color(0xFFFF6D01), // Orange
    Color(0xFF46BDC6), // Teal
    Color(0xFF7B1FA2), // Purple
    Color(0xFFD81B60), // Pink
    Color(0xFF00897B), // Dark Teal
    Color(0xFF5C6BC0), // Indigo
    Color(0xFFFFA000), // Amber
    Color(0xFF8D6E63), // Brown
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChartSection(
    tickerValues: List<Pair<String, Double>>,
    currencyFormat: NumberFormat
) {
    var expanded by remember { mutableStateOf(true) }
    val totalValue = tickerValues.sumOf { it.second }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Chart", style = MaterialTheme.typography.titleMedium)
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pie chart
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .aspectRatio(1f)
                    ) {
                        val diameter = size.minDimension
                        val topLeft = Offset(
                            (size.width - diameter) / 2f,
                            (size.height - diameter) / 2f
                        )
                        var startAngle = -90f
                        tickerValues.forEachIndexed { index, (_, value) ->
                            val sweep = (value / totalValue * 360.0).toFloat()
                            drawArc(
                                color = chartColors[index % chartColors.size],
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = true,
                                topLeft = topLeft,
                                size = Size(diameter, diameter)
                            )
                            startAngle += sweep
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Legend
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tickerValues.forEachIndexed { index, (ticker, value) ->
                            val pct = if (totalValue > 0) value / totalValue * 100 else 0.0
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(chartColors[index % chartColors.size])
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$ticker ${"%.1f".format(pct)}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total: ${currencyFormat.format(totalValue)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PositionFormDialog(
    viewModel: PositionViewModel,
    existing: PositionEntity?,
    onDismiss: () -> Unit,
    onSave: (ticker: String, quantity: Double, cost: Double, accountId: Long?) -> Unit
) {
    val items by viewModel.availableItems.collectAsStateWithLifecycle()
    val positions by viewModel.positions.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val tickerItems = items.filter { !it.ticker.isNullOrBlank() }
    val existingKeys = positions.map { "${it.ticker.uppercase()}:${it.accountId}" }.toSet()

    var tickerInput by remember { mutableStateOf(existing?.ticker ?: "") }
    var quantity by remember { mutableStateOf(existing?.quantity?.toString() ?: "") }
    var cost by remember { mutableStateOf(existing?.cost?.toString() ?: "") }
    var selectedAccountId by remember { mutableStateOf(existing?.accountId) }
    var tickerExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val isEditing = existing != null
    val filteredSuggestions = tickerItems.filter {
        it.ticker!!.contains(tickerInput, ignoreCase = true) ||
                it.name.contains(tickerInput, ignoreCase = true)
    }
    val selectedAccountName = selectedAccountId?.let { id ->
        accounts.find { it.id == id }?.name ?: "Select account"
    } ?: if (accounts.isNotEmpty()) "Default (${accounts.first().name})" else "No accounts"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Position" else "Add Position") },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = tickerExpanded && !isEditing && filteredSuggestions.isNotEmpty(),
                    onExpandedChange = { if (!isEditing) tickerExpanded = it }
                ) {
                    OutlinedTextField(
                        value = tickerInput,
                        onValueChange = {
                            tickerInput = it.uppercase()
                            tickerExpanded = true
                            error = null
                        },
                        readOnly = isEditing,
                        label = { Text("Ticker") },
                        placeholder = { Text("e.g. AAPL, MSFT") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = tickerExpanded && !isEditing && filteredSuggestions.isNotEmpty(),
                        onDismissRequest = { tickerExpanded = false }
                    ) {
                        filteredSuggestions.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.ticker} — ${item.name}") },
                                onClick = {
                                    tickerInput = item.ticker!!
                                    tickerExpanded = false
                                    error = null
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedAccountName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    selectedAccountId = account.id
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it; error = null },
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it; error = null },
                    label = { Text("Cost (USD)") },
                    singleLine = true,
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val ticker = tickerInput.trim().uppercase()
                val qty = quantity.toDoubleOrNull()
                val c = cost.toDoubleOrNull()
                val resolvedAccountId = selectedAccountId ?: accounts.firstOrNull()?.id
                val key = "${ticker}:${resolvedAccountId}"
                when {
                    ticker.isBlank() -> error = "Enter a ticker"
                    accounts.isEmpty() -> error = "Create an account first"
                    !isEditing && key in existingKeys -> error = "$ticker already exists in this account"
                    qty == null || qty <= 0 -> error = "Enter a valid quantity"
                    c == null || c < 0 -> error = "Enter a valid cost"
                    else -> onSave(ticker, qty, c, selectedAccountId)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
