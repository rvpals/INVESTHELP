package com.investhelp.app.ui.item

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.data.local.entity.InvestmentAccountEntity
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
    val isEditing = !ticker.isNullOrBlank()

    if (isEditing) {
        LaunchedEffect(ticker) {
            viewModel.loadItem(ticker!!)
        }
    }

    val itemRows by viewModel.selectedItemRows.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    // Find the specific row being edited (if accountId given) or first row
    val existingItem = if (isEditing) {
        if (accountId != null && accountId != -1L) {
            itemRows.find { it.accountId == accountId }
        } else {
            itemRows.firstOrNull()
        }
    } else null

    var name by remember { mutableStateOf("") }
    var tickerInput by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(InvestmentType.Stock) }
    var currentPrice by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(existingItem) {
        if (isEditing && existingItem != null && !initialized) {
            name = existingItem.name
            tickerInput = existingItem.ticker
            selectedType = existingItem.type
            currentPrice = existingItem.currentPrice.toString()
            initialized = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Item" else "New Item") },
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
                readOnly = isEditing,
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

            OutlinedTextField(
                value = currentPrice,
                onValueChange = { currentPrice = it },
                label = { Text("Current Price per Share") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val price = currentPrice.toDoubleOrNull() ?: 0.0
                    val resolvedTicker = tickerInput.trim().uppercase()
                    if (resolvedTicker.isNotBlank()) {
                        // Update metadata for all rows of this ticker
                        viewModel.saveItem(
                            ticker = resolvedTicker,
                            accountId = existingItem?.accountId ?: accounts.firstOrNull()?.id ?: return@Button,
                            name = name,
                            type = selectedType,
                            currentPrice = price,
                            quantity = existingItem?.quantity ?: 0.0,
                            cost = existingItem?.cost ?: 0.0
                        )
                        onSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && tickerInput.isNotBlank()
            ) {
                Text(if (isEditing) "Update" else "Create")
            }
        }
    }
}
