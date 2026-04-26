package com.investhelp.app.ui.transaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.data.local.entity.InvestmentTransactionEntity
import com.investhelp.app.ui.components.ConfirmDeleteDialog
import com.investhelp.app.ui.settings.SettingsViewModel
import androidx.compose.ui.platform.LocalContext
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionViewModel,
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit
) {
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val currentPrices by viewModel.currentPrices.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val context = LocalContext.current
    val warnBeforeDelete = remember {
        context.getSharedPreferences(SettingsViewModel.PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getBoolean(SettingsViewModel.KEY_WARN_BEFORE_DELETE, true)
    }
    var transactionToDelete by remember { mutableStateOf<Long?>(null) }
    var selectedAccountId by rememberSaveable { mutableLongStateOf(-1L) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    // Multi-select state
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val inSelectionMode = selectedIds.isNotEmpty()
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }

    val filteredTransactions = if (selectedAccountId == -1L) {
        transactions
    } else {
        transactions.filter { it.accountId == selectedAccountId }
    }

    val allFilteredSelected = filteredTransactions.isNotEmpty() &&
            filteredTransactions.all { it.id in selectedIds }

    // Single delete confirm
    val deleteTarget = transactionToDelete?.let { id -> transactions.find { it.id == id } }
    if (deleteTarget != null) {
        ConfirmDeleteDialog(
            title = "Delete Transaction",
            message = "Are you sure you want to delete this ${deleteTarget.action.name} transaction for ${deleteTarget.numberOfShares} shares of ${deleteTarget.ticker}?",
            onConfirm = {
                viewModel.deleteTransaction(deleteTarget)
                transactionToDelete = null
            },
            onDismiss = { transactionToDelete = null }
        )
    }

    // Bulk delete confirm
    if (showBulkDeleteConfirm) {
        val count = selectedIds.size
        ConfirmDeleteDialog(
            title = "Delete $count Transactions",
            message = "Are you sure you want to delete $count selected transactions?",
            onConfirm = {
                val toDelete = transactions.filter { it.id in selectedIds }
                viewModel.deleteTransactions(toDelete)
                selectedIds = emptySet()
                showBulkDeleteConfirm = false
            },
            onDismiss = { showBulkDeleteConfirm = false }
        )
    }

    Scaffold(
        topBar = {
            if (inSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            selectedIds = if (allFilteredSelected) {
                                emptySet()
                            } else {
                                filteredTransactions.map { it.id }.toSet()
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                        }
                        IconButton(onClick = {
                            if (warnBeforeDelete) {
                                showBulkDeleteConfirm = true
                            } else {
                                val toDelete = transactions.filter { it.id in selectedIds }
                                viewModel.deleteTransactions(toDelete)
                                selectedIds = emptySet()
                            }
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete selected",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Transactions") },
                    actions = {
                        TextButton(onClick = onAddTransaction) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text("Add Trans")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Account filter
            ExposedDropdownMenuBox(
                expanded = accountDropdownExpanded,
                onExpandedChange = { accountDropdownExpanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = if (selectedAccountId == -1L) "All Accounts"
                    else accounts.find { it.id == selectedAccountId }?.name ?: "All Accounts",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filter by Account") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = accountDropdownExpanded,
                    onDismissRequest = { accountDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Accounts") },
                        onClick = {
                            selectedAccountId = -1L
                            accountDropdownExpanded = false
                        }
                    )
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

            if (filteredTransactions.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No transactions yet", style = MaterialTheme.typography.bodyLarge)
                    Text("Tap Add Trans to add one", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { transaction ->
                        val accountName = accounts.find { it.id == transaction.accountId }?.name ?: "Unknown"
                        val timeStr = transaction.time?.format(timeFormatter)?.let { " $it" } ?: ""
                        val isSelected = transaction.id in selectedIds

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (inSelectionMode) {
                                            selectedIds = if (isSelected) {
                                                selectedIds - transaction.id
                                            } else {
                                                selectedIds + transaction.id
                                            }
                                        } else {
                                            onEditTransaction(transaction.id)
                                        }
                                    },
                                    onLongClick = {
                                        selectedIds = if (isSelected) {
                                            selectedIds - transaction.id
                                        } else {
                                            selectedIds + transaction.id
                                        }
                                    }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.secondaryContainer
                                else
                                    CardDefaults.cardColors().containerColor
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (inSelectionMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            selectedIds = if (isSelected) {
                                                selectedIds - transaction.id
                                            } else {
                                                selectedIds + transaction.id
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = transaction.action.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (transaction.action.name == "Buy")
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = transaction.ticker,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    Text(
                                        text = "$accountName | ${transaction.date.format(dateFormatter)}$timeStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${transaction.numberOfShares} shares @ ${currencyFormat.format(transaction.pricePerShare)}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (transaction.totalAmount != 0.0) {
                                        Text(
                                            text = "Total: ${currencyFormat.format(transaction.totalAmount)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    currentPrices[transaction.ticker]?.let { price ->
                                        val gainLoss = (price - transaction.pricePerShare) * transaction.numberOfShares
                                        val sign = if (gainLoss >= 0) "+" else ""
                                        Text(
                                            text = "G/L: $sign${currencyFormat.format(gainLoss)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (gainLoss >= 0)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.error
                                        )
                                    }
                                    if (transaction.note.isNotBlank()) {
                                        Text(
                                            text = transaction.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (!inSelectionMode) {
                                    IconButton(onClick = {
                                        if (warnBeforeDelete) {
                                            transactionToDelete = transaction.id
                                        } else {
                                            viewModel.deleteTransaction(transaction)
                                        }
                                    }) {
                                        Icon(
                                            Icons.Default.Close,
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
}
