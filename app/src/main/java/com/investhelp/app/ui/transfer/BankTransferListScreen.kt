package com.investhelp.app.ui.transfer

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.ui.components.ConfirmDeleteDialog
import com.investhelp.app.ui.settings.SettingsViewModel
import androidx.compose.ui.platform.LocalContext
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankTransferListScreen(
    viewModel: BankTransferViewModel,
    onAddTransfer: () -> Unit,
    onEditTransfer: (Long) -> Unit
) {
    val transfers by viewModel.allTransfers.collectAsStateWithLifecycle()
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val context = LocalContext.current
    val warnBeforeDelete = remember {
        context.getSharedPreferences(SettingsViewModel.PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getBoolean(SettingsViewModel.KEY_WARN_BEFORE_DELETE, true)
    }
    var transferToDelete by remember { mutableStateOf<Long?>(null) }

    val deleteTarget = transferToDelete?.let { id -> transfers.find { it.id == id } }
    if (deleteTarget != null) {
        val accountName = accounts.find { it.id == deleteTarget.accountId }?.name ?: "Unknown"
        ConfirmDeleteDialog(
            title = "Delete Transfer",
            message = "Delete transfer of ${currencyFormat.format(deleteTarget.amount)} to $accountName on ${deleteTarget.date.format(dateFormatter)}?",
            onConfirm = {
                viewModel.deleteTransfer(deleteTarget)
                transferToDelete = null
            },
            onDismiss = { transferToDelete = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bank Transfers") },
                actions = {
                    TextButton(onClick = onAddTransfer) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Add Transfer")
                    }
                }
            )
        }
    ) { padding ->
        if (transfers.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No bank transfers yet", style = MaterialTheme.typography.bodyLarge)
                Text("Tap Add Transfer to add one", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total per account summary
                item {
                    val accountMap = accounts.associateBy { it.id }
                    val totalsByAccount = transfers
                        .groupBy { it.accountId }
                        .map { (accountId, list) ->
                            (accountMap[accountId]?.name ?: "Unknown") to list.sumOf { it.amount }
                        }
                        .sortedByDescending { it.second }
                    val grandTotal = transfers.sumOf { it.amount }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Total Transfers by Account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            totalsByAccount.forEach { (accountName, total) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(accountName, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        currencyFormat.format(total),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Grand Total",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    currencyFormat.format(grandTotal),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                items(transfers, key = { it.id }) { transfer ->
                    val accountName = accounts.find { it.id == transfer.accountId }?.name ?: "Unknown"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onEditTransfer(transfer.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currencyFormat.format(transfer.amount),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "To: $accountName | ${transfer.date.format(dateFormatter)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (transfer.note.isNotBlank()) {
                                    Text(
                                        text = transfer.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = {
                                if (warnBeforeDelete) {
                                    transferToDelete = transfer.id
                                } else {
                                    viewModel.deleteTransfer(transfer)
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
