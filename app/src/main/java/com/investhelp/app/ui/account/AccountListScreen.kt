package com.investhelp.app.ui.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.data.local.entity.AccountPerformanceEntity
import com.investhelp.app.ui.components.CollapsibleCard
import com.investhelp.app.ui.components.ConfirmDeleteDialog
import com.investhelp.app.ui.settings.SettingsViewModel
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
    viewModel: AccountViewModel,
    onNavigateToAccount: (Long) -> Unit,
    onAddAccount: () -> Unit,
    onNavigateToPerformance: () -> Unit
) {
    val accounts by viewModel.accountsWithValues.collectAsStateWithLifecycle()
    val allPerformance by viewModel.allPerformanceByAccount.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(SettingsViewModel.PREFS_NAME, android.content.Context.MODE_PRIVATE)
    }
    val warnBeforeDelete = remember {
        prefs.getBoolean(SettingsViewModel.KEY_WARN_BEFORE_DELETE, true)
    }
    var accountToDelete by remember { mutableStateOf<Long?>(null) }

    if (accountToDelete != null) {
        val account = accounts.find { it.account.id == accountToDelete }
        if (account != null) {
            ConfirmDeleteDialog(
                title = "Delete Account",
                message = "Delete \"${account.account.name}\"? All associated transactions will also be deleted.",
                onConfirm = {
                    viewModel.deleteAccount(account.account)
                    accountToDelete = null
                },
                onDismiss = { accountToDelete = null }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Accounts") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAccount) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
            }
        }
    ) { padding ->
        if (accounts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No accounts yet", style = MaterialTheme.typography.bodyLarge)
                Text("Tap + to add one", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(accounts, key = { it.account.id }) { accountWithValue ->
                    val pinKey = "pin_card_account_${accountWithValue.account.id}"
                    var pinned by remember {
                        mutableStateOf(prefs.getBoolean(pinKey, false))
                    }
                    val accountRecords = allPerformance[accountWithValue.account.id] ?: emptyList()

                    CollapsibleCard(
                        title = accountWithValue.account.name,
                        pinned = pinned,
                        onPinToggle = { newPinned ->
                            pinned = newPinned
                            prefs.edit().putBoolean(pinKey, newPinned).apply()
                        }
                    ) {
                        Column {
                            if (accountWithValue.account.description.isNotBlank()) {
                                Text(
                                    text = accountWithValue.account.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Text(
                                text = currencyFormat.format(accountWithValue.currentValue),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            accountWithValue.account.lastValue?.let { lastVal ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Last: ${currencyFormat.format(lastVal)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            PerformancePanel(
                                records = accountRecords,
                                onNavigateToPerformance = onNavigateToPerformance
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { onNavigateToAccount(accountWithValue.account.id) }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = {
                                    if (warnBeforeDelete) {
                                        accountToDelete = accountWithValue.account.id
                                    } else {
                                        viewModel.deleteAccount(accountWithValue.account)
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
    }
}

@Composable
private fun PerformancePanel(
    records: List<AccountPerformanceEntity>,
    onNavigateToPerformance: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MM/dd/yyyy") }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Performance",
                style = MaterialTheme.typography.titleSmall
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand"
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                if (records.size >= 2) {
                    MiniPerformanceChart(
                        records = records,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    Text(
                        text = "Not enough data for chart (need 2+ records)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val last10 = records.sortedByDescending { it.date }.take(10)
                if (last10.isNotEmpty()) {
                    // Grid header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Date",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Value",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    last10.forEach { record ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = record.date.format(dateFormatter),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = currencyFormat.format(record.totalValue),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onNavigateToPerformance,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Performance Detail")
                }
            }
        }
    }
}

@Composable
private fun MiniPerformanceChart(
    records: List<AccountPerformanceEntity>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padding = 8f

        val values = records.map { it.totalValue }
        val minVal = values.min() * 0.998
        val maxVal = values.max() * 1.002
        val valRange = (maxVal - minVal).let { if (it < 0.01) 1.0 else it }

        // Draw grid lines
        for (i in 0..3) {
            val y = padding + (h - 2 * padding) * i / 3f
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }

        // Draw line path
        val path = Path()
        records.forEachIndexed { index, record ->
            val x = padding + (w - 2 * padding) * index / (records.size - 1).coerceAtLeast(1)
            val y = padding + (h - 2 * padding) * (1f - ((record.totalValue - minVal) / valRange).toFloat())
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round))

        // Draw points
        records.forEachIndexed { index, record ->
            val x = padding + (w - 2 * padding) * index / (records.size - 1).coerceAtLeast(1)
            val y = padding + (h - 2 * padding) * (1f - ((record.totalValue - minVal) / valRange).toFloat())
            drawCircle(lineColor, radius = 4f, center = Offset(x, y))
        }
    }
}
