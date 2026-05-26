package com.investhelp.app.ui.item

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.model.InvestmentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemFormScreen(
    ticker: String?,
    accountId: Long?,
    viewModel: ItemViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val hasTicker = !ticker.isNullOrBlank()

    if (hasTicker) {
        LaunchedEffect(ticker) {
            viewModel.loadItem(ticker!!)
        }
    }

    val existingItem by viewModel.selectedItem.collectAsStateWithLifecycle()
    val fetchedPrice by viewModel.fetchedPrice.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var tickerInput by remember { mutableStateOf(ticker?.uppercase() ?: "") }
    var selectedType by remember { mutableStateOf(InvestmentType.Stock) }
    var currentPrice by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }
    val isExistingPosition = existingItem != null

    LaunchedEffect(existingItem) {
        if (hasTicker && existingItem != null && !initialized) {
            name = existingItem!!.name
            tickerInput = existingItem!!.ticker
            selectedType = existingItem!!.type
            currentPrice = existingItem!!.currentPrice.toString()
            quantity = existingItem!!.quantity.toString()
            initialized = true
        }
    }

    // Auto-fetch price from Yahoo if ticker is provided but not in DB
    LaunchedEffect(hasTicker, existingItem) {
        if (hasTicker && existingItem == null && !initialized) {
            tickerInput = ticker!!.uppercase()
            viewModel.fetchPriceForTicker(ticker)
            initialized = true
        }
    }

    val fetchedName by viewModel.fetchedName.collectAsStateWithLifecycle()

    LaunchedEffect(fetchedPrice) {
        fetchedPrice?.let { currentPrice = it.toString() }
    }

    LaunchedEffect(fetchedName) {
        fetchedName?.let { if (name.isBlank()) name = it }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.clearFetchedPrice() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isExistingPosition) "Edit Item" else "New Item") },
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
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Item Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = tickerInput,
                onValueChange = { tickerInput = it.uppercase() },
                label = { Text("Ticker Symbol") },
                placeholder = { Text("e.g. AAPL, MSFT") },
                singleLine = true,
                readOnly = hasTicker,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedType.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    InvestmentType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name) },
                            onClick = {
                                selectedType = type
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = currentPrice,
                    onValueChange = { currentPrice = it },
                    label = { Text("Current Price per Share") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val t = tickerInput.trim()
                        if (t.isNotBlank()) viewModel.fetchPriceForTicker(t)
                    },
                    enabled = tickerInput.isNotBlank(),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Fetch")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                Column {
                    IconButton(
                        onClick = {
                            val current = quantity.toDoubleOrNull() ?: 0.0
                            quantity = (current + 1).let {
                                if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
                            }
                        }
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase quantity")
                    }
                    IconButton(
                        onClick = {
                            val current = quantity.toDoubleOrNull() ?: 0.0
                            if (current >= 1) {
                                quantity = (current - 1).let {
                                    if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease quantity")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val price = currentPrice.toDoubleOrNull() ?: 0.0
                    val qty = quantity.toDoubleOrNull() ?: 0.0
                    val resolvedTicker = tickerInput.trim().uppercase()
                    if (resolvedTicker.isNotBlank() && qty > 0) {
                        viewModel.saveItem(
                            ticker = resolvedTicker,
                            name = name.ifBlank { resolvedTicker },
                            type = selectedType,
                            currentPrice = price,
                            quantity = qty
                        )
                        onSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = tickerInput.isNotBlank() && (quantity.toDoubleOrNull() ?: 0.0) > 0.0
            ) {
                Text(if (isExistingPosition) "Update" else "Save")
            }
        }
    }
}
