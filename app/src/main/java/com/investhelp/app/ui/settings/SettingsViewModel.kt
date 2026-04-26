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
import com.investhelp.app.data.local.dao.AccountPerformanceDao
import com.investhelp.app.data.local.dao.CsvImportMappingDao
import com.investhelp.app.data.local.entity.AccountPerformanceEntity
import com.investhelp.app.data.local.entity.CsvImportMappingEntity
import com.investhelp.app.model.BackupAccount
import com.investhelp.app.model.BackupBankTransfer
import com.investhelp.app.model.BackupData
import com.investhelp.app.model.BackupItem
import com.investhelp.app.model.BackupTransaction
import com.investhelp.app.model.CsvImportType
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
    val columnMappings: Map<Int, String> = emptyMap(),
    val dateFormats: Map<Int, String> = emptyMap(),
    val selectedAccountId: Long = -1L,
    val isImporting: Boolean = false,
    val importProgress: Float = 0f,
    val importTotal: Int = 0,
    val importCurrent: Int = 0,
    val importType: CsvImportType = CsvImportType.Position
)

data class CsvMappingDialogState(
    val importType: CsvImportType = CsvImportType.Position,
    val csvHeaders: List<String> = emptyList(),
    val previewRows: List<List<String>> = emptyList(),
    val columnMappings: Map<Int, String> = emptyMap(),
    val dateFormats: Map<Int, String> = emptyMap(),
    val hasSavedMapping: Boolean = false
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
    val csvMappingDialog: CsvMappingDialogState? = null,
    val accounts: List<InvestmentAccountEntity> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountDao: InvestmentAccountDao,
    private val itemDao: InvestmentItemDao,
    private val transactionDao: InvestmentTransactionDao,
    private val bankTransferDao: BankTransferDao,
    private val accountPerformanceDao: AccountPerformanceDao,
    private val csvMappingDao: CsvImportMappingDao
) : ViewModel() {

    companion object {
        const val PREFS_NAME = "invest_help_settings"
        const val KEY_AUTO_UPDATE_SHARES = "auto_update_shares"
        const val KEY_WARN_BEFORE_DELETE = "warn_before_delete"
        const val KEY_MARKET_INDICES = "market_indices"
        const val KEY_BACKUP_FOLDER_URI = "backup_folder_uri"

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
                for (i in backupData.items) {
                    val ticker = i.ticker.ifBlank { "UNKNOWN" }
                    if (backupData.version >= 2) {
                        itemDao.upsertItem(
                            InvestmentItemEntity(
                                ticker = ticker,
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
                        itemDao.upsertItem(
                            InvestmentItemEntity(
                                ticker = ticker,
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

    fun openMappingDialog(importType: CsvImportType, fileUri: Uri) {
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

                val saved = csvMappingDao.getMapping(importType.name)
                var mappings = emptyMap<Int, String>()
                var dateFormats = emptyMap<Int, String>()
                var hasSaved = false

                if (saved != null) {
                    hasSaved = true
                    mappings = deserializeMappings(saved.mappingsJson, headers)
                    dateFormats = deserializeDateFormats(saved.dateFormatJson, headers)
                } else {
                    val autoMappings = mutableMapOf<Int, String>()
                    val aliases = mapOf(
                        "price" to "currentPrice",
                        "current price" to "currentPrice",
                        "last price" to "currentPrice",
                        "description" to "name",
                        "security" to "name",
                        "security name" to "name",
                        "symbol" to "ticker",
                        "unrealized g/l amt." to "totalGainLoss",
                        "unrealized g/l" to "totalGainLoss",
                        "unrealized gain/loss" to "totalGainLoss",
                        "gain/loss" to "totalGainLoss",
                        "today's value change" to "dayGainLoss",
                        "shares" to "quantity",
                        "qty" to "quantity",
                        "cost basis" to "cost",
                        "market value" to "value",
                        "unit cost" to "currentPrice",
                        "number of shares" to "numberOfShares",
                        "price per share" to "pricePerShare",
                        "account" to "accountName",
                        "account name" to "accountName",
                        "total" to "totalAmount",
                        "total amount" to "totalAmount",
                        "amount" to "totalAmount",
                    )
                    val usedFields = mutableSetOf<String>()
                    headers.forEachIndexed { index, header ->
                        val normalized = header.trim().lowercase()
                        val match = importType.mappableFields.firstOrNull { it.lowercase() == normalized }
                            ?: aliases[normalized]?.takeIf { it in importType.mappableFields }
                        if (match != null && match !in usedFields) {
                            autoMappings[index] = match
                            usedFields.add(match)
                        }
                    }
                    mappings = autoMappings
                }

                _uiState.value = _uiState.value.copy(
                    csvMappingDialog = CsvMappingDialogState(
                        importType = importType,
                        csvHeaders = headers,
                        previewRows = rows,
                        columnMappings = mappings,
                        dateFormats = dateFormats,
                        hasSavedMapping = hasSaved
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "CSV parse failed: ${e.message}"
                )
            }
        }
    }

    fun updateMappingDialogField(colIndex: Int, fieldName: String) {
        val current = _uiState.value.csvMappingDialog ?: return
        val newMappings = current.columnMappings.toMutableMap()
        if (fieldName == "Skip") {
            newMappings.remove(colIndex)
        } else {
            val existingKey = newMappings.entries.find { it.value == fieldName }?.key
            if (existingKey != null) newMappings.remove(existingKey)
            newMappings[colIndex] = fieldName
        }
        _uiState.value = _uiState.value.copy(
            csvMappingDialog = current.copy(columnMappings = newMappings)
        )
    }

    fun updateMappingDialogDateFormat(colIndex: Int, format: String) {
        val current = _uiState.value.csvMappingDialog ?: return
        val newFormats = current.dateFormats.toMutableMap()
        if (format.isBlank()) {
            newFormats.remove(colIndex)
        } else {
            newFormats[colIndex] = format
        }
        _uiState.value = _uiState.value.copy(
            csvMappingDialog = current.copy(dateFormats = newFormats)
        )
    }

    fun saveMappingDialog() {
        val dialog = _uiState.value.csvMappingDialog ?: return
        viewModelScope.launch {
            val mappingsJson = serializeMappings(dialog.columnMappings, dialog.csvHeaders)
            val dateFormatJson = serializeDateFormats(dialog.dateFormats, dialog.csvHeaders)
            csvMappingDao.upsertMapping(
                CsvImportMappingEntity(
                    importType = dialog.importType.name,
                    mappingsJson = mappingsJson,
                    dateFormatJson = dateFormatJson
                )
            )
            _uiState.value = _uiState.value.copy(
                csvMappingDialog = null,
                message = "Mapping saved for ${dialog.importType.label}."
            )
        }
    }

    fun dismissMappingDialog() {
        _uiState.value = _uiState.value.copy(csvMappingDialog = null)
    }

    private fun serializeMappings(mappings: Map<Int, String>, headers: List<String>): String {
        return mappings.entries.joinToString(";") { (idx, field) ->
            val headerName = headers.getOrElse(idx) { "col$idx" }
            "$headerName=$field"
        }
    }

    private fun serializeDateFormats(formats: Map<Int, String>, headers: List<String>): String {
        return formats.entries.joinToString(";") { (idx, fmt) ->
            val headerName = headers.getOrElse(idx) { "col$idx" }
            "$headerName=$fmt"
        }
    }

    private fun deserializeMappings(json: String, headers: List<String>): Map<Int, String> {
        if (json.isBlank()) return emptyMap()
        val result = mutableMapOf<Int, String>()
        json.split(";").forEach { entry ->
            val parts = entry.split("=", limit = 2)
            if (parts.size == 2) {
                val headerName = parts[0]
                val field = parts[1]
                val idx = headers.indexOfFirst { it.equals(headerName, ignoreCase = true) }
                if (idx >= 0) result[idx] = field
            }
        }
        return result
    }

    private fun deserializeDateFormats(json: String, headers: List<String>): Map<Int, String> {
        if (json.isBlank()) return emptyMap()
        val result = mutableMapOf<Int, String>()
        json.split(";").forEach { entry ->
            val parts = entry.split("=", limit = 2)
            if (parts.size == 2) {
                val headerName = parts[0]
                val fmt = parts[1]
                val idx = headers.indexOfFirst { it.equals(headerName, ignoreCase = true) }
                if (idx >= 0) result[idx] = fmt
            }
        }
        return result
    }

    fun startCsvImport(importType: CsvImportType, fileUri: Uri, accountId: Long) {
        viewModelScope.launch {
            val saved = csvMappingDao.getMapping(importType.name)
            if (saved == null) {
                _uiState.value = _uiState.value.copy(
                    message = "No mapping defined for ${importType.label}. Please define mapping first."
                )
                return@launch
            }

            try {
                val allRows = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(fileUri)?.use { input ->
                        val reader = BufferedReader(InputStreamReader(input))
                        val headerLine = reader.readLine()
                            ?: throw Exception("CSV file is empty.")
                        val headers = parseCsvLine(headerLine)
                        val colCount = headers.size
                        val rows = mutableListOf<List<String>>()
                        var line = reader.readLine()
                        while (line != null) {
                            if (line.isNotBlank()) {
                                val parsed = parseCsvLine(line)
                                if (parsed.size >= colCount / 2) rows.add(parsed)
                            }
                            line = reader.readLine()
                        }
                        headers to rows
                    } ?: throw Exception("Cannot read CSV file.")
                }

                val headers = allRows.first
                val rows = allRows.second
                val mappings = deserializeMappings(saved.mappingsJson, headers)
                val dateFormats = deserializeDateFormats(saved.dateFormatJson, headers)
                val total = rows.size

                _uiState.value = _uiState.value.copy(
                    csvImport = CsvImportState(
                        importType = importType,
                        isImporting = true,
                        importTotal = total,
                        importCurrent = 0,
                        importProgress = 0f
                    )
                )

                var imported = 0
                var skipped = 0
                val log = StringBuilder()

                for (row in rows) {
                    val fieldValues = mutableMapOf<String, String>()
                    val fieldDateFormats = mutableMapOf<String, String>()
                    for ((colIndex, fieldName) in mappings) {
                        if (colIndex < row.size) {
                            fieldValues[fieldName] = row[colIndex].trim()
                        }
                        dateFormats[colIndex]?.let { fmt ->
                            fieldDateFormats[fieldName] = fmt
                        }
                    }

                    try {
                        when (importType) {
                            CsvImportType.Transaction -> importTransactionRow(fieldValues, fieldDateFormats, accountId)
                            CsvImportType.Position -> importPositionRow(fieldValues, accountId)
                            CsvImportType.Performance -> importPerformanceRow(fieldValues, fieldDateFormats, accountId)
                        }
                        imported++
                    } catch (e: Exception) {
                        skipped++
                        if (skipped <= 5) log.appendLine("Row ${imported + skipped}: ${e.message}")
                    }

                    _uiState.value = _uiState.value.copy(
                        csvImport = _uiState.value.csvImport?.copy(
                            importCurrent = imported + skipped,
                            importProgress = (imported + skipped).toFloat() / total
                        )
                    )
                }

                val summary = "Imported $imported ${importType.label.lowercase()}." +
                        if (skipped > 0) " Skipped $skipped rows." else ""
                val fullMsg = if (log.isNotEmpty()) "$summary\n$log" else summary
                _uiState.value = _uiState.value.copy(
                    csvImport = null,
                    message = fullMsg
                )
                com.investhelp.app.AppLog.log(summary)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    csvImport = null,
                    message = "Import failed: ${e.message}"
                )
            }
        }
    }

    private suspend fun importTransactionRow(
        fields: Map<String, String>,
        dateFormats: Map<String, String>,
        defaultAccountId: Long
    ) {
        val ticker = fields["ticker"]?.uppercase()
        if (ticker.isNullOrBlank()) throw Exception("Missing ticker")
        val shares = parseNumeric(fields["numberOfShares"])
            ?: throw Exception("Missing numberOfShares")
        val price = parseNumeric(fields["pricePerShare"])
            ?: throw Exception("Missing pricePerShare")
        val actionStr = fields["action"]?.trim() ?: "Buy"
        val action = try {
            TransactionAction.valueOf(actionStr.replaceFirstChar { it.uppercase() })
        } catch (_: Exception) { TransactionAction.Buy }

        val dateFmt = dateFormats["date"]?.takeIf { it.isNotBlank() }?.let {
            DateTimeFormatter.ofPattern(it)
        } ?: DateTimeFormatter.ofPattern("MM/dd/yyyy")
        val date = fields["date"]?.let {
            try { LocalDate.parse(it.trim(), dateFmt) } catch (_: Exception) { null }
        } ?: LocalDate.now()

        val timeFmt = dateFormats["time"]?.takeIf { it.isNotBlank() }?.let {
            DateTimeFormatter.ofPattern(it)
        } ?: DateTimeFormatter.ofPattern("HH:mm")
        val time = fields["time"]?.let {
            try { LocalTime.parse(it.trim(), timeFmt) } catch (_: Exception) { null }
        }

        val accountName = fields["accountName"]?.trim()
        val accountId = if (!accountName.isNullOrBlank()) {
            accountDao.getAllAccountsSnapshot().find {
                it.name.equals(accountName, ignoreCase = true)
            }?.id ?: defaultAccountId
        } else defaultAccountId

        val totalAmount = parseNumeric(fields["totalAmount"]) ?: 0.0
        val note = fields["note"]?.trim() ?: ""

        transactionDao.insertTransaction(
            InvestmentTransactionEntity(
                date = date, time = time, action = action, accountId = accountId,
                ticker = ticker, numberOfShares = shares, pricePerShare = price,
                totalAmount = totalAmount, note = note
            )
        )

        if (itemDao.getItemByTicker(ticker) == null) {
            itemDao.upsertItem(
                InvestmentItemEntity(
                    ticker = ticker, name = ticker,
                    type = InvestmentType.Stock, currentPrice = price,
                    quantity = 0.0, cost = 0.0, dayGainLoss = 0.0,
                    totalGainLoss = 0.0, value = 0.0
                )
            )
        }
    }

    private suspend fun importPositionRow(
        fields: Map<String, String>,
        defaultAccountId: Long
    ) {
        val ticker = fields["ticker"]?.uppercase()
        if (ticker.isNullOrBlank()) throw Exception("Missing ticker")

        val existing = itemDao.getItemByTicker(ticker)
        val item = InvestmentItemEntity(
            ticker = ticker,
            name = fields["name"]?.ifBlank { null } ?: existing?.name ?: ticker,
            type = fields["type"]?.let {
                runCatching { InvestmentType.valueOf(it) }.getOrNull()
            } ?: existing?.type ?: InvestmentType.Stock,
            currentPrice = parseNumeric(fields["currentPrice"])
                ?: existing?.currentPrice ?: 0.0,
            quantity = parseNumeric(fields["quantity"])
                ?: existing?.quantity ?: 0.0,
            cost = parseNumeric(fields["cost"])
                ?: existing?.cost ?: 0.0,
            dayGainLoss = parseNumeric(fields["dayGainLoss"])
                ?: existing?.dayGainLoss ?: 0.0,
            totalGainLoss = parseNumeric(fields["totalGainLoss"])
                ?: existing?.totalGainLoss ?: 0.0,
            value = parseNumeric(fields["value"])
                ?: existing?.value ?: 0.0,
            dayHigh = existing?.dayHigh ?: 0.0,
            dayLow = existing?.dayLow ?: 0.0
        )
        itemDao.upsertItem(item)
    }

    private suspend fun importPerformanceRow(
        fields: Map<String, String>,
        dateFormats: Map<String, String>,
        defaultAccountId: Long
    ) {
        val totalValue = parseNumeric(fields["totalValue"])
            ?: throw Exception("Missing totalValue")

        val dateFmt = dateFormats["date"]?.takeIf { it.isNotBlank() }?.let {
            DateTimeFormatter.ofPattern(it)
        } ?: DateTimeFormatter.ofPattern("MM/dd/yyyy")
        val date = fields["date"]?.let {
            try { LocalDate.parse(it.trim(), dateFmt) } catch (_: Exception) { null }
        } ?: LocalDate.now()

        val accountName = fields["accountName"]?.trim()
        val accountId = if (!accountName.isNullOrBlank()) {
            accountDao.getAllAccountsSnapshot().find {
                it.name.equals(accountName, ignoreCase = true)
            }?.id ?: defaultAccountId
        } else defaultAccountId

        val note = fields["note"]?.trim() ?: ""

        accountPerformanceDao.insertRecord(
            AccountPerformanceEntity(
                accountId = accountId,
                totalValue = totalValue,
                dateTime = date.atTime(LocalTime.now()),
                note = note
            )
        )
    }

    private fun parseNumeric(value: String?): Double? =
        value?.replace(",", "")?.toDoubleOrNull()

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
