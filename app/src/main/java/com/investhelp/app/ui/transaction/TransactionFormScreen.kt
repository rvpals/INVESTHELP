package com.investhelp.app.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.model.TransactionAction
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    transactionId: Long?,
    viewModel: TransactionViewModel,
    selectedPrice: String? = null,
    onAnalyzePrice: (String) -> Unit = {},
    onClearSelectedPrice: () -> Unit = {},
    onViewItem: (String) -> Unit = {},
    onSimulate: (ticker: String, shares: Double, days: Int) -> Unit = { _, _, _ -> },
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val isEditing = transactionId != null
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val existingTransaction by viewModel.selectedTransaction.collectAsStateWithLifecycle()

    if (isEditing) {
        LaunchedEffect(transactionId) {
            viewModel.loadTransaction(transactionId!!)
        }
    }

    var date by rememberSaveable(stateSaver = Saver<LocalDate, Long>(
        save = { it.toEpochDay() },
        restore = { LocalDate.ofEpochDay(it) }
    )) { mutableStateOf(LocalDate.now()) }
    var time by rememberSaveable(stateSaver = Saver<LocalTime?, Int>(
        save = { it?.toSecondOfDay() ?: -1 },
        restore = { if (it == -1) null else LocalTime.ofSecondOfDay(it.toLong()) }
    )) { mutableStateOf<LocalTime?>(null) }
    var action by rememberSaveable(stateSaver = Saver<TransactionAction, String>(
        save = { it.name },
        restore = { TransactionAction.valueOf(it) }
    )) { mutableStateOf(TransactionAction.Buy) }
    var selectedAccountId by rememberSaveable(stateSaver = Saver<Long?, Long>(
        save = { it ?: -1L },
        restore = { if (it == -1L) null else it }
    )) { mutableStateOf<Long?>(null) }
    var ticker by rememberSaveable { mutableStateOf("") }
    var numberOfShares by rememberSaveable { mutableStateOf("") }
    var pricePerShare by rememberSaveable { mutableStateOf("") }
    var totalAmount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }
    var initialized by rememberSaveable { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    // Computed total for display
    val shares = numberOfShares.toDoubleOrNull() ?: 0.0
    val price = pricePerShare.toDoubleOrNull() ?: 0.0
    val computedTotal = shares * price

    // Auto-select first account for new transactions
    LaunchedEffect(accounts) {
        if (!isEditing && selectedAccountId == null && accounts.isNotEmpty()) {
            selectedAccountId = accounts.first().id
        }
    }

    LaunchedEffect(existingTransaction) {
        if (isEditing && existingTransaction != null && !initialized) {
            date = existingTransaction!!.date
            time = existingTransaction!!.time
            action = existingTransaction!!.action
            selectedAccountId = existingTransaction!!.accountId
            ticker = existingTransaction!!.ticker
            numberOfShares = existingTransaction!!.numberOfShares.toString()
            pricePerShare = existingTransaction!!.pricePerShare.toString()
            totalAmount = if (existingTransaction!!.totalAmount != 0.0)
                existingTransaction!!.totalAmount.toString() else ""
            note = existingTransaction!!.note
            initialized = true
        }
    }

    LaunchedEffect(selectedPrice) {
        if (selectedPrice != null) {
            pricePerShare = selectedPrice
            onClearSelectedPrice()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Transaction" else "New Transaction") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            // Date
            OutlinedTextField(
                value = date.format(dateFormatter),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth(),
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    .also { interactionSource ->
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect {
                                if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                    showDatePicker = true
                                }
                            }
                        }
                    }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Time (optional) with "Now" button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = time?.format(timeFormatter) ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Time (optional)") },
                    modifier = Modifier.weight(1f),
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        .also { interactionSource ->
                            LaunchedEffect(interactionSource) {
                                interactionSource.interactions.collect {
                                    if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                        showTimePicker = true
                                    }
                                }
                            }
                        }
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = { time = LocalTime.now() }) {
                    Text("Now")
                }
                if (time != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedButton(onClick = { time = null }) {
                        Text("Clear")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Account dropdown
            ExposedDropdownMenuBox(
                expanded = accountExpanded,
                onExpandedChange = { accountExpanded = it }
            ) {
                OutlinedTextField(
                    value = accounts.find { it.id == selectedAccountId }?.name ?: "",
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

            // Action toggle
            Text("Action", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransactionAction.entries.forEach { txAction ->
                    FilterChip(
                        selected = action == txAction,
                        onClick = { action = txAction },
                        label = { Text(txAction.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ticker
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = ticker,
                    onValueChange = { ticker = it.uppercase() },
                    label = { Text("Ticker") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { if (ticker.isNotBlank()) onViewItem(ticker.trim()) },
                    enabled = ticker.isNotBlank()
                ) {
                    Text("View")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Number of Shares
            OutlinedTextField(
                value = numberOfShares,
                onValueChange = { numberOfShares = it },
                label = { Text("Number of Shares") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = pricePerShare,
                    onValueChange = { pricePerShare = it },
                    label = { Text("Price") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { if (ticker.isNotBlank()) onAnalyzePrice(ticker.trim()) },
                    enabled = ticker.isNotBlank()
                ) {
                    Text("Analyze Price")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Computed Total display + Total Amount input with copy button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Calculated: ${currencyFormat.format(computedTotal)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    totalAmount = if (computedTotal == 0.0) "" else computedTotal.toString()
                }) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy calculated total",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            OutlinedTextField(
                value = totalAmount,
                onValueChange = { totalAmount = it },
                label = { Text("Total Amount") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Simulate button - shows price movement from transaction date to now
            val daysSinceTransaction = ChronoUnit.DAYS.between(date, LocalDate.now()).toInt()
            Button(
                onClick = {
                    val sharesVal = numberOfShares.toDoubleOrNull() ?: 0.0
                    onSimulate(ticker.trim(), sharesVal, daysSinceTransaction)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = ticker.isNotBlank() && numberOfShares.toDoubleOrNull() != null && daysSinceTransaction > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Simulate")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val sharesVal = numberOfShares.toDoubleOrNull() ?: 0.0
                    val priceVal = pricePerShare.toDoubleOrNull() ?: 0.0
                    val totalVal = totalAmount.toDoubleOrNull() ?: 0.0
                    if (selectedAccountId != null && ticker.isNotBlank()) {
                        viewModel.saveTransaction(
                            date = date,
                            time = time,
                            action = action,
                            accountId = selectedAccountId!!,
                            ticker = ticker.trim(),
                            numberOfShares = sharesVal,
                            pricePerShare = priceVal,
                            totalAmount = totalVal,
                            note = note.trim(),
                            existingId = transactionId
                        )
                        onSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedAccountId != null && ticker.isNotBlank() &&
                        numberOfShares.toDoubleOrNull() != null && pricePerShare.toDoubleOrNull() != null
            ) {
                Text(if (isEditing) "Update" else "Create")
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        val currentTime = time ?: LocalTime.now()
        val timePickerState = rememberTimePickerState(
            initialHour = currentTime.hour,
            initialMinute = currentTime.minute
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}
