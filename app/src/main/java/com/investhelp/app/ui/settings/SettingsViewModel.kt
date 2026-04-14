package com.investhelp.app.ui.settings

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.local.dao.BankTransferDao
import com.investhelp.app.data.local.dao.InvestmentAccountDao
import com.investhelp.app.data.local.dao.InvestmentItemDao
import com.investhelp.app.data.local.dao.InvestmentTransactionDao
import com.investhelp.app.data.local.entity.BankTransferEntity
import com.investhelp.app.data.local.entity.InvestmentAccountEntity
import com.investhelp.app.data.local.entity.InvestmentItemEntity
import com.investhelp.app.data.local.entity.InvestmentTransactionEntity
import com.investhelp.app.model.BackupAccount
import com.investhelp.app.model.BackupBankTransfer
import com.investhelp.app.model.BackupData
import com.investhelp.app.model.BackupItem
import com.investhelp.app.model.BackupTransaction
import com.investhelp.app.model.InvestmentType
import com.investhelp.app.model.TransactionAction
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class CsvImportState(
    val csvHeaders: List<String> = emptyList(),
    val previewRows: List<List<String>> = emptyList(),
    val columnMappings: Map<Int, String> = emptyMap(), // csvColumnIndex -> app field name
    val selectedAccountId: Long = -1L,
    val isImporting: Boolean = false,
    val importProgress: Float = 0f,
    val importTotal: Int = 0,
    val importCurrent: Int = 0
)

data class SettingsUiState(
    val backupFolderUri: Uri? = null,
    val backupFolderName: String? = null,
    val message: String? = null,
    val isExporting: Boolean = false,
    val isRestoring: Boolean = false,
    val autoUpdateShares: Boolean = false,
    val warnBeforeDelete: Boolean = true,
    val enabledMarketIndices: Set<String> = SettingsViewModel.DEFAULT_MARKET_INDICES,
    val csvImport: CsvImportState? = null,
    val accounts: List<InvestmentAccountEntity> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountDao: InvestmentAccountDao,
    private val itemDao: InvestmentItemDao,
    private val transactionDao: InvestmentTransactionDao,
    private val bankTransferDao: BankTransferDao
) : ViewModel() {

    companion object {
        const val PREFS_NAME = "invest_help_settings"
        const val KEY_AUTO_UPDATE_SHARES = "auto_update_shares"
        const val KEY_WARN_BEFORE_DELETE = "warn_before_delete"
        const val KEY_MARKET_INDICES = "market_indices"
        const val KEY_BACKUP_FOLDER_URI = "backup_folder_uri"
        val IMPORTABLE_FIELDS = listOf(
            "Skip",
            "ticker",
            "name",
            "type",
            "currentPrice",
            "quantity",
            "cost",
            "dayGainLoss",
            "totalGainLoss",
            "value"
        )

        data class MarketIndexConfig(
            val symbol: String,
            val label: String
        )

        val AVAILABLE_MARKET_INDICES = listOf(
            MarketIndexConfig("^IXIC", "NASDAQ"),
            MarketIndexConfig("^GSPC", "S&P 500"),
            MarketIndexConfig("^DJI", "Dow"),
            MarketIndexConfig("GC=F", "Gold"),
            MarketIndexConfig("^RUT", "Russell 2K"),
            MarketIndexConfig("SI=F", "Silver"),
            MarketIndexConfig("CL=F", "Oil"),
            MarketIndexConfig("BTC-USD", "Bitcoin")
        )

        val DEFAULT_MARKET_INDICES = setOf("^IXIC", "^GSPC", "^DJI", "GC=F")
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val savedBackupUri: Uri? = prefs.getString(KEY_BACKUP_FOLDER_URI, null)?.let { Uri.parse(it) }

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            autoUpdateShares = prefs.getBoolean(KEY_AUTO_UPDATE_SHARES, false),
            warnBeforeDelete = prefs.getBoolean(KEY_WARN_BEFORE_DELETE, true),
            enabledMarketIndices = prefs.getStringSet(KEY_MARKET_INDICES, null)
                ?: DEFAULT_MARKET_INDICES,
            backupFolderUri = savedBackupUri,
            backupFolderName = savedBackupUri?.let {
                DocumentFile.fromTreeUri(context, it)?.name ?: it.lastPathSegment
            }
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val json = Json { prettyPrint = true }

    init {
        viewModelScope.launch {
            val accounts = accountDao.getAllAccountsSnapshot()
            _uiState.value = _uiState.value.copy(accounts = accounts)
        }
    }

    fun setAutoUpdateShares(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_UPDATE_SHARES, enabled).apply()
        _uiState.value = _uiState.value.copy(autoUpdateShares = enabled)
    }

    fun setWarnBeforeDelete(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WARN_BEFORE_DELETE, enabled).apply()
        _uiState.value = _uiState.value.copy(warnBeforeDelete = enabled)
    }

    fun toggleMarketIndex(symbol: String, enabled: Boolean) {
        val current = _uiState.value.enabledMarketIndices.toMutableSet()
        if (enabled) current.add(symbol) else current.remove(symbol)
        prefs.edit().putStringSet(KEY_MARKET_INDICES, current).apply()
        _uiState.value = _uiState.value.copy(enabledMarketIndices = current)
    }

    fun setBackupFolder(uri: Uri) {
        prefs.edit().putString(KEY_BACKUP_FOLDER_URI, uri.toString()).apply()
        val docFile = DocumentFile.fromTreeUri(context, uri)
        _uiState.value = _uiState.value.copy(
            backupFolderUri = uri,
            backupFolderName = docFile?.name ?: uri.lastPathSegment,
            message = null
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun exportData() {
        val folderUri = _uiState.value.backupFolderUri
        if (folderUri == null) {
            _uiState.value = _uiState.value.copy(message = "Please select a backup folder first.")
            return
        }

        _uiState.value = _uiState.value.copy(isExporting = true, message = null)

        viewModelScope.launch {
            try {
                val accounts = accountDao.getAllAccountsSnapshot()
                val items = itemDao.getAllItemsSnapshot()
                val transactions = transactionDao.getAllTransactionsSnapshot()
                val bankTransfers = bankTransferDao.getAllTransfersSnapshot()

                val backupData = BackupData(
                    accounts = accounts.map {
                        BackupAccount(it.id, it.name, it.description, it.initialValue)
                    },
                    items = items.map {
                        BackupItem(
                            ticker = it.ticker,
                            accountId = it.accountId,
                            name = it.name,
                            type = it.type.name,
                            currentPrice = it.currentPrice,
                            quantity = it.quantity,
                            cost = it.cost,
                            dayGainLoss = it.dayGainLoss,
                            totalGainLoss = it.totalGainLoss,
                            value = it.value,
                            dayHigh = it.dayHigh,
                            dayLow = it.dayLow
                        )
                    },
                    transactions = transactions.map {
                        BackupTransaction(
                            it.id, it.date.toEpochDay(), it.time?.toSecondOfDay(),
                            it.action.name, it.accountId, it.ticker,
                            it.numberOfShares, it.pricePerShare,
                            it.totalAmount, it.note
                        )
                    },
                    bankTransfers = bankTransfers.map {
                        BackupBankTransfer(
                            it.id, it.date.toEpochDay(), it.amount, it.accountId, it.note
                        )
                    }
                )

                val jsonString = json.encodeToString(BackupData.serializer(), backupData)
                val timestamp = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
                )
                val fileName = "invest_help_backup_$timestamp.json"

                withContext(Dispatchers.IO) {
                    val folder = DocumentFile.fromTreeUri(context, folderUri)
                        ?: throw Exception("Cannot access backup folder.")
                    val file = folder.createFile("application/json", fileName)
                        ?: throw Exception("Cannot create backup file.")
                    context.contentResolver.openOutputStream(file.uri)?.use { out ->
                        out.write(jsonString.toByteArray())
                    } ?: throw Exception("Cannot write to backup file.")
                }

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    message = "Exported to $fileName"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    message = "Export failed: ${e.message}"
                )
            }
        }
    }

    fun restoreData(fileUri: Uri) {
        _uiState.value = _uiState.value.copy(isRestoring = true, message = null)

        viewModelScope.launch {
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(fileUri)?.use { input ->
                        input.bufferedReader().readText()
                    } ?: throw Exception("Cannot read backup file.")
                }

                val backupData = json.decodeFromString(BackupData.serializer(), jsonString)

                // Delete in reverse dependency order
                bankTransferDao.deleteAll()
                transactionDao.deleteAll()
                itemDao.deleteAll()
                accountDao.deleteAll()

                // Insert in dependency order
                for (a in backupData.accounts) {
                    accountDao.insertAccount(
                        InvestmentAccountEntity(a.id, a.name, a.description, a.initialValue)
                    )
                }
                val firstAccountId = backupData.accounts.firstOrNull()?.id ?: 1L
                for (i in backupData.items) {
                    if (backupData.version >= 2) {
                        // v2 format: all fields present
                        itemDao.upsertItem(
                            InvestmentItemEntity(
                                ticker = i.ticker,
                                accountId = i.accountId,
                                name = i.name,
                                type = InvestmentType.valueOf(i.type),
                                currentPrice = i.currentPrice,
                                quantity = i.quantity,
                                cost = i.cost,
                                dayGainLoss = i.dayGainLoss,
                                totalGainLoss = i.totalGainLoss,
                                value = i.value,
                                dayHigh = i.dayHigh,
                                dayLow = i.dayLow
                            )
                        )
                    } else {
                        // v1 format: assign to first account, use numShares as quantity
                        val ticker = i.ticker.ifBlank { "UNKNOWN" }
                        itemDao.upsertItem(
                            InvestmentItemEntity(
                                ticker = ticker,
                                accountId = firstAccountId,
                                name = i.name,
                                type = InvestmentType.valueOf(i.type),
                                currentPrice = i.currentPrice,
                                quantity = i.numShares,
                                cost = 0.0,
                                dayGainLoss = 0.0,
                                totalGainLoss = 0.0,
                                value = i.numShares * i.currentPrice
                            )
                        )
                    }
                }
                for (t in backupData.transactions) {
                    transactionDao.insertTransaction(
                        InvestmentTransactionEntity(
                            t.id,
                            LocalDate.ofEpochDay(t.dateEpochDay),
                            t.timeSecondOfDay?.let { LocalTime.ofSecondOfDay(it.toLong()) },
                            TransactionAction.valueOf(t.action),
                            t.accountId, t.ticker,
                            t.numberOfShares, t.pricePerShare,
                            t.totalAmount, t.note
                        )
                    )
                }
                for (bt in backupData.bankTransfers) {
                    bankTransferDao.insertTransfer(
                        BankTransferEntity(
                            bt.id,
                            LocalDate.ofEpochDay(bt.dateEpochDay),
                            bt.amount, bt.accountId, bt.note
                        )
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isRestoring = false,
                    message = "Restored ${backupData.accounts.size} accounts, ${backupData.items.size} items, ${backupData.transactions.size} transactions, ${backupData.bankTransfers.size} bank transfers."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRestoring = false,
                    message = "Restore failed: ${e.message}"
                )
            }
        }
    }

    // --- CSV Import ---

    fun parseCsvFile(fileUri: Uri) {
        viewModelScope.launch {
            try {
                val (headers, rows) = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(fileUri)?.use { input ->
                        val reader = BufferedReader(InputStreamReader(input))
                        val headerLine = reader.readLine()
                            ?: throw Exception("CSV file is empty.")
                        val parsedHeaders = parseCsvLine(headerLine)
                        val previewRows = mutableListOf<List<String>>()
                        repeat(3) {
                            val line = reader.readLine() ?: return@repeat
                            previewRows.add(parseCsvLine(line))
                        }
                        parsedHeaders to previewRows
                    } ?: throw Exception("Cannot read CSV file.")
                }

                // Auto-map columns whose headers match field names (case-insensitive)
                val autoMappings = mutableMapOf<Int, String>()
                headers.forEachIndexed { index, header ->
                    val normalized = header.trim().lowercase()
                    val match = IMPORTABLE_FIELDS.firstOrNull { it.lowercase() == normalized }
                    if (match != null) autoMappings[index] = match
                }

                val firstAccount = _uiState.value.accounts.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    csvImport = CsvImportState(
                        csvHeaders = headers,
                        previewRows = rows,
                        columnMappings = autoMappings,
                        selectedAccountId = firstAccount?.id ?: -1L
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "CSV parse failed: ${e.message}"
                )
            }
        }
    }

    fun updateCsvMapping(columnIndex: Int, fieldName: String) {
        val current = _uiState.value.csvImport ?: return
        val newMappings = current.columnMappings.toMutableMap()
        if (fieldName == "Skip") {
            newMappings.remove(columnIndex)
        } else {
            // Remove any other column that was mapped to the same field
            val existingKey = newMappings.entries.find { it.value == fieldName }?.key
            if (existingKey != null) newMappings.remove(existingKey)
            newMappings[columnIndex] = fieldName
        }
        _uiState.value = _uiState.value.copy(
            csvImport = current.copy(columnMappings = newMappings)
        )
    }

    fun setCsvImportAccount(accountId: Long) {
        val current = _uiState.value.csvImport ?: return
        _uiState.value = _uiState.value.copy(
            csvImport = current.copy(selectedAccountId = accountId)
        )
    }

    fun dismissCsvImport() {
        _uiState.value = _uiState.value.copy(csvImport = null)
    }

    fun executeCsvImport(fileUri: Uri) {
        val importState = _uiState.value.csvImport ?: return
        val mappings = importState.columnMappings
        if (!mappings.containsValue("ticker")) {
            _uiState.value = _uiState.value.copy(
                message = "You must map a column to 'ticker'."
            )
            return
        }
        val accountId = importState.selectedAccountId
        if (accountId == -1L) {
            _uiState.value = _uiState.value.copy(
                message = "Please select an account."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            csvImport = importState.copy(isImporting = true, importProgress = 0f)
        )

        viewModelScope.launch {
            try {
                val allRows = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(fileUri)?.use { input ->
                        val reader = BufferedReader(InputStreamReader(input))
                        reader.readLine() // skip header
                        val rows = mutableListOf<List<String>>()
                        var line = reader.readLine()
                        while (line != null) {
                            rows.add(parseCsvLine(line))
                            line = reader.readLine()
                        }
                        rows
                    } ?: throw Exception("Cannot read CSV file.")
                }

                val total = allRows.size
                _uiState.value = _uiState.value.copy(
                    csvImport = _uiState.value.csvImport?.copy(importTotal = total)
                )

                var imported = 0
                for (row in allRows) {
                    val fieldValues = mutableMapOf<String, String>()
                    for ((colIndex, fieldName) in mappings) {
                        if (colIndex < row.size) {
                            fieldValues[fieldName] = row[colIndex].trim()
                        }
                    }

                    val ticker = fieldValues["ticker"]
                    if (ticker.isNullOrBlank()) {
                        imported++
                        continue
                    }

                    val existing = itemDao.getItem(ticker, accountId)
                    val item = InvestmentItemEntity(
                        ticker = ticker,
                        accountId = accountId,
                        name = fieldValues["name"]?.ifBlank { null }
                            ?: existing?.name ?: ticker,
                        type = fieldValues["type"]?.let { typeName ->
                            runCatching { InvestmentType.valueOf(typeName) }.getOrNull()
                        } ?: existing?.type ?: InvestmentType.Stock,
                        currentPrice = fieldValues["currentPrice"]?.toDoubleOrNull()
                            ?: existing?.currentPrice ?: 0.0,
                        quantity = fieldValues["quantity"]?.toDoubleOrNull()
                            ?: existing?.quantity ?: 0.0,
                        cost = fieldValues["cost"]?.toDoubleOrNull()
                            ?: existing?.cost ?: 0.0,
                        dayGainLoss = fieldValues["dayGainLoss"]?.toDoubleOrNull()
                            ?: existing?.dayGainLoss ?: 0.0,
                        totalGainLoss = fieldValues["totalGainLoss"]?.toDoubleOrNull()
                            ?: existing?.totalGainLoss ?: 0.0,
                        value = fieldValues["value"]?.toDoubleOrNull()
                            ?: existing?.value ?: 0.0,
                        dayHigh = existing?.dayHigh ?: 0.0,
                        dayLow = existing?.dayLow ?: 0.0
                    )
                    itemDao.upsertItem(item)

                    imported++
                    _uiState.value = _uiState.value.copy(
                        csvImport = _uiState.value.csvImport?.copy(
                            importCurrent = imported,
                            importProgress = imported.toFloat() / total
                        )
                    )
                }

                _uiState.value = _uiState.value.copy(
                    csvImport = null,
                    message = "Imported $imported positions."
                )
                // Refresh accounts in case values changed
                val accounts = accountDao.getAllAccountsSnapshot()
                _uiState.value = _uiState.value.copy(accounts = accounts)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    csvImport = _uiState.value.csvImport?.copy(isImporting = false),
                    message = "Import failed: ${e.message}"
                )
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && !inQuotes -> inQuotes = true
                c == '"' && inQuotes -> {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
