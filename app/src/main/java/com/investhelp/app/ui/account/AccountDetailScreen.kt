package com.investhelp.app.ui.account

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.data.local.entity.InvestmentTransactionEntity
import com.investhelp.app.data.local.entity.PositionEntity
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: Long,
    viewModel: AccountViewModel,
    onEditAccount: () -> Unit,
    onNavigateToTransaction: (Long) -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(accountId) {
        viewModel.loadAccount(accountId)
    }

    val account by viewModel.selectedAccount.collectAsStateWithLifecycle()
    val transactions by viewModel.accountTransactions.collectAsStateWithLifecycle()
    val positions by viewModel.accountPositions.collectAsStateWithLifecycle()
    val currentValue by viewModel.currentValue.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Positions", "Transactions")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(account?.name ?: "Account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEditAccount) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Account summary card
            account?.let { acc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (acc.description.isNotBlank()) {
                            Text(
                                text = acc.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Initial Value", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    currencyFormat.format(acc.initialValue),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Column {
                                Text("Current Value", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    currencyFormat.format(currentValue),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tabs
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Tab content
            when (selectedTab) {
                0 -> PositionsTab(positions, currencyFormat)
                1 -> TransactionsTab(transactions, currencyFormat, dateFormatter, timeFormatter, onNavigateToTransaction)
            }
        }
    }
}

@Composable
private fun PositionsTab(
    positions: List<PositionEntity>,
    currencyFormat: NumberFormat
) {
    if (positions.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("No positions for this account", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(positions, key = { it.ticker }) { position ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = position.ticker,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = currencyFormat.format(position.value),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${"%.4f".format(position.quantity)} shares",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Cost: ${currencyFormat.format(position.cost)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val dayColor = if (position.dayGainLoss >= 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                            val totalColor = if (position.totalGainLoss >= 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                            Text(
                                text = "Day: ${currencyFormat.format(position.dayGainLoss)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = dayColor
                            )
                            Text(
                                text = "Total: ${currencyFormat.format(position.totalGainLoss)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = totalColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionsTab(
    transactions: List<InvestmentTransactionEntity>,
    currencyFormat: NumberFormat,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter,
    onNavigateToTransaction: (Long) -> Unit
) {
    if (transactions.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("No transactions for this account", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(transactions, key = { it.id }) { transaction ->
                val timeStr = transaction.time?.format(timeFormatter)?.let { " $it" } ?: ""
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigateToTransaction(transaction.id) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
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
                                text = "${transaction.date.format(dateFormatter)}$timeStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${transaction.numberOfShares} shares",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "@ ${currencyFormat.format(transaction.pricePerShare)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (transaction.totalAmount != 0.0) {
                                Text(
                                    text = currencyFormat.format(transaction.totalAmount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
