package com.investhelp.app.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.ui.components.ConfirmDeleteDialog
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionViewModel,
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit
) {
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    var transactionToDelete by remember { mutableStateOf<Long?>(null) }

    val deleteTarget = transactionToDelete?.let { id -> transactions.find { it.id == id } }
    if (deleteTarget != null) {
        ConfirmDeleteDialog(
            title = "Delete Transaction",
            message = "Are you sure you want to delete this transaction?",
            onConfirm = {
                viewModel.deleteTransaction(deleteTarget)
                transactionToDelete = null
            },
            onDismiss = { transactionToDelete = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Transactions") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        if (transactions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No transactions yet", style = MaterialTheme.typography.bodyLarge)
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
                items(transactions, key = { it.id }) { transaction ->
                    val accountName = accounts.find { it.id == transaction.accountId }?.name ?: "Unknown"
                    val timeStr = transaction.time?.format(timeFormatter)?.let { " $it" } ?: ""

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onEditTransaction(transaction.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                if (transaction.note.isNotBlank()) {
                                    Text(
                                        text = transaction.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { transactionToDelete = transaction.id }) {
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
