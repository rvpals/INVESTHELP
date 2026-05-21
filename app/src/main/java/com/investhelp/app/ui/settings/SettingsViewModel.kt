package com.investhelp.app.ui.settings

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.local.dao.DefinitionDao
import com.investhelp.app.data.local.dao.InvestmentAccountDao
import com.investhelp.app.data.local.dao.InvestmentItemDao
import com.investhelp.app.data.local.dao.InvestmentTransactionDao
import com.investhelp.app.data.local.entity.InvestmentAccountEntity
import com.investhelp.app.data.local.entity.InvestmentItemEntity
import com.investhelp.app.data.local.entity.InvestmentTransactionEntity
import com.investhelp.app.data.local.dao.AccountPerformanceDao
import com.investhelp.app.data.local.dao.CsvImportMappingDao
import com.investhelp.app.data.local.entity.AccountPerformanceEntity
import com.investhelp.app.data.local.entity.CsvImportMappingEntity
import com.investhelp.app.data.local.entity.DefinitionEntity
import com.investhelp.app.data.local.entity.NamedCsvMappingEntity
import com.investhelp.app.model.BackupAccount
import com.investhelp.app.model.BackupData
import com.investhelp.app.model.BackupItem
import com.investhelp.app.model.BackupTransaction
import com.investhelp.app.model.CsvImportType
import com.investhelp.app.ui.theme.AppTheme
import com.investhelp.app.ui.theme.ThemePreferences
import com.investhelp.app.model.InvestmentType
import com.investhelp.app.model.TransactionAction
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.investhelp.app.AutoRefreshWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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

data class PositionMappingScreenState(
    val csvHeaders: List<String> = emptyList(),
    val previewRows: List<List<String>> = emptyList(),
    val columnMappings: Map<Int, String> = emptyMap(),
    val dateFormats: Map<Int, String> = emptyMap(),
    val hasSavedMapping: Boolean = false,
    val savedMappings: List<NamedCsvMappingEntity> = emptyList()
)

data class ImportLogEntry(
    val ticker: String,
    val status: ImportStatus,
    val details: String = ""
)

enum class ImportStatus { IMPORTED, UPDATED, SKIPPED }

data class PositionImportResult(
    val entries: List<ImportLogEntry> = emptyList(),
    val totalImported: Int = 0,
    val totalUpdated: Int = 0,
    val totalSkipped: Int = 0
)

data class SettingsUiState(
    val backupFolderUri: Uri? = null,
    val backupFolderName: String? = null,
    val message: String? = null,
    val isExporting: Boolean = false,
    val isRestoring: Boolean = false,
    val autoUpdateShares: Boolean = false,
    val autoUpdateChangeHistory: Boolean = false,
    val autoRefreshEnabled: Boolean = false,
    val autoRefreshInterval: String = "30m",
    val warnBeforeDelete: Boolean = true,
    val selectedTheme: AppTheme = AppTheme.Default,
    val enabledMarketIndices: Set<String> = SettingsViewModel.DEFAULT_MARKET_INDICES,
    val marketIndicesOrder: List<String> = SettingsViewModel.AVAILABLE_MARKET_INDICES.map { it.symbol },
    val csvImport: CsvImportState? = null,
    val csvMappingDialog: CsvMappingDialogState? = null,
    val positionMappingScreen: PositionMappingScreenState? = null,
    val positionImportResult: PositionImportResult? = null,
    val showMappingSelectionDialog: Boolean = false,
    val savedPositionMappings: List<NamedCsvMappingEntity> = emptyList(),
    val pendingImportFileUri: Uri? = null,
    val accounts: List<InvestmentAccountEntity> = emptyList(),
    val dashboardCardOrder: List<String> = SettingsViewModel.DEFAULT_CARD_ORDER,
    val watchListCardVisible: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountDao: InvestmentAccountDao,
    private val itemDao: InvestmentItemDao,
    private val transactionDao: InvestmentTransactionDao,
    private val accountPerformanceDao: AccountPerformanceDao,
    private val csvMappingDao: CsvImportMappingDao,
    private val definitionDao: DefinitionDao
) : ViewModel() {

    companion object {
        const val PREFS_NAME = "invest_help_settings"
        const val KEY_AUTO_UPDATE_SHARES = "auto_update_shares"
        const val KEY_WARN_BEFORE_DELETE = "warn_before_delete"
        const val KEY_MARKET_INDICES = "market_indices"
        const val KEY_MARKET_INDICES_ORDER = "market_indices_order"
        const val KEY_AUTO_UPDATE_CHANGE_HISTORY = "auto_update_change_history"
        const val KEY_BACKUP_FOLDER_URI = "backup_folder_uri"
        const val KEY_THEME = "app_theme"
        const val KEY_AUTO_REFRESH_ENABLED = "auto_refresh_enabled"
        const val KEY_AUTO_REFRESH_INTERVAL = "auto_refresh_interval"

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

        const val KEY_CARD_VISIBLE_WATCH_LIST = "card_visible_watch_list"
        const val KEY_DASHBOARD_CARD_ORDER = "dashboard_card_order"
        val DEFAULT_CARD_ORDER = listOf(
            "portfolio_summary", "market_indices", "daily_glance", "watch_list"
        )
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val savedBackupUri: Uri? = prefs.getString(KEY_BACKUP_FOLDER_URI, null)?.let { Uri.parse(it) }

    private val defaultOrder = AVAILABLE_MARKET_INDICES.map { it.symbol }

    private fun loadMarketIndicesOrder(): List<String> {
        val saved = prefs.getString(KEY_MARKET_INDICES_ORDER, null)
        if (saved.isNullOrBlank()) return defaultOrder
        val ordered = saved.split(",").filter { it.isNotBlank() }
        val allSymbols = defaultOrder.toSet()
        val missing = allSymbols - ordered.toSet()
        return (ordered.filter { it in allSymbols } + missing)
    }

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            autoUpdateShares = prefs.getBoolean(KEY_AUTO_UPDATE_SHARES, false),
            autoUpdateChangeHistory = prefs.getBoolean(KEY_AUTO_UPDATE_CHANGE_HISTORY, false),
            autoRefreshEnabled = prefs.getBoolean(KEY_AUTO_REFRESH_ENABLED, false),
            autoRefreshInterval = prefs.getString(KEY_AUTO_REFRESH_INTERVAL, "30m") ?: "30m",
            warnBeforeDelete = prefs.getBoolean(KEY_WARN_BEFORE_DELETE, true),
            selectedTheme = AppTheme.fromName(prefs.getString(KEY_THEME, AppTheme.Default.name) ?: AppTheme.Default.name),
            enabledMarketIndices = prefs.getStringSet(KEY_MARKET_INDICES, null)
                ?: DEFAULT_MARKET_INDICES,
            marketIndicesOrder = loadMarketIndicesOrder(),
            backupFolderUri = savedBackupUri,
            backupFolderName = savedBackupUri?.let {
                DocumentFile.fromTreeUri(context, it)?.name ?: it.lastPathSegment
            },
            dashboardCardOrder = prefs.getString(KEY_DASHBOARD_CARD_ORDER, null)
                ?.split(",")?.filter { it.isNotBlank() }
                ?: DEFAULT_CARD_ORDER,
            watchListCardVisible = prefs.getBoolean(KEY_CARD_VISIBLE_WATCH_LIST, true)
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

    fun setAutoUpdateChangeHistory(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_UPDATE_CHANGE_HISTORY, enabled).apply()
        _uiState.value = _uiState.value.copy(autoUpdateChangeHistory = enabled)
    }

    fun setAutoRefreshEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_REFRESH_ENABLED, enabled).apply()
        _uiState.value = _uiState.value.copy(autoRefreshEnabled = enabled)
        if (enabled) {
            scheduleAutoRefresh(_uiState.value.autoRefreshInterval)
        } else {
            cancelAutoRefresh()
        }
    }

    fun setAutoRefreshInterval(interval: String) {
        prefs.edit().putString(KEY_AUTO_REFRESH_INTERVAL, interval).apply()
        _uiState.value = _uiState.value.copy(autoRefreshInterval = interval)
        if (_uiState.value.autoRefreshEnabled) {
            scheduleAutoRefresh(interval)
        }
    }

    private fun scheduleAutoRefresh(interval: String) {
        val (repeatMinutes, flexMinutes) = when (interval) {
            "5m" -> 15L to 5L
            "30m" -> 30L to 10L
            "1h" -> 60L to 15L
            "5h" -> 300L to 30L
            "market_close" -> 1440L to 60L
            else -> 30L to 10L
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<AutoRefreshWorker>(
            repeatMinutes, java.util.concurrent.TimeUnit.MINUTES,
            flexMinutes, java.util.concurrent.TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            AutoRefreshWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun cancelAutoRefresh() {
        WorkManager.getInstance(context).cancelUniqueWork(AutoRefreshWorker.WORK_NAME)
    }

    fun setWarnBeforeDelete(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WARN_BEFORE_DELETE, enabled).apply()
        _uiState.value = _uiState.value.copy(warnBeforeDelete = enabled)
    }

    fun setTheme(theme: AppTheme) {
        ThemePreferences.setTheme(context, theme)
        _uiState.value = _uiState.value.copy(selectedTheme = theme)
    }

    fun toggleMarketIndex(symbol: String, enabled: Boolean) {
        val current = _uiState.value.enabledMarketIndices.toMutableSet()
        if (enabled) current.add(symbol) else current.remove(symbol)
        prefs.edit().putStringSet(KEY_MARKET_INDICES, current).apply()
        _uiState.value = _uiState.value.copy(enabledMarketIndices = current)
    }

    fun moveMarketIndex(symbol: String, direction: Int) {
        val order = _uiState.value.marketIndicesOrder.toMutableList()
        val idx = order.indexOf(symbol)
        if (idx < 0) return
        val newIdx = idx + direction
        if (newIdx < 0 || newIdx >= order.size) return
        order[idx] = order[newIdx].also { order[newIdx] = order[idx] }
        prefs.edit().putString(KEY_MARKET_INDICES_ORDER, order.joinToString(",")).apply()
        _uiState.value = _uiState.value.copy(marketIndicesOrder = order)
    }

    fun moveDashboardCard(cardKey: String, direction: Int) {
        val order = _uiState.value.dashboardCardOrder.toMutableList()
        val idx = order.indexOf(cardKey)
        if (idx < 0) return
        val newIdx = idx + direction
        if (newIdx < 0 || newIdx >= order.size) return
        order[idx] = order[newIdx].also { order[newIdx] = order[idx] }
        prefs.edit().putString(KEY_DASHBOARD_CARD_ORDER, order.joinToString(",")).apply()
        _uiState.value = _uiState.value.copy(dashboardCardOrder = order)
    }

    fun setWatchListCardVisible(visible: Boolean) {
        prefs.edit().putBoolean(KEY_CARD_VISIBLE_WATCH_LIST, visible).apply()
        _uiState.value = _uiState.value.copy(watchListCardVisible = visible)
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

    fun clearTable(importType: CsvImportType) {
        viewModelScope.launch {
            when (importType) {
                CsvImportType.Transaction -> transactionDao.deleteAll()
                CsvImportType.Position -> itemDao.deleteAll()
                CsvImportType.Performance -> accountPerformanceDao.deleteAll()
            }
            _uiState.value = _uiState.value.copy(
                message = "${importType.label} cleared successfully."
            )
        }
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
                            it.action.name, ticker = it.ticker,
                            numberOfShares = it.numberOfShares,
                            pricePerShare = it.pricePerShare,
                            totalAmount = it.totalAmount, note = it.note
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
                            t.ticker,
                            t.numberOfShares, t.pricePerShare,
                            t.totalAmount, t.note
                        )
                    )
                }
                _uiState.value = _uiState.value.copy(
                    isRestoring = false,
                    message = "Restored ${backupData.accounts.size} accounts, ${backupData.items.size} items, ${backupData.transactions.size} transactions."
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

    // --- Position Mapping Full Screen ---

    fun openPositionMappingScreen(fileUri: Uri) {
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

                val saved = csvMappingDao.getMapping(CsvImportType.Position.name)
                var mappings = emptyMap<Int, String>()
                var hasSaved = false

                if (saved != null) {
                    hasSaved = true
                    mappings = deserializeMappings(saved.mappingsJson, headers)
                } else {
                    mappings = autoMapHeaders(headers, CsvImportType.Position)
                }

                val namedMappings = csvMappingDao.getNamedMappings(CsvImportType.Position.name)

                _uiState.value = _uiState.value.copy(
                    positionMappingScreen = PositionMappingScreenState(
                        csvHeaders = headers,
                        previewRows = rows,
                        columnMappings = mappings,
                        hasSavedMapping = hasSaved,
                        savedMappings = namedMappings
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "CSV parse failed: ${e.message}"
                )
            }
        }
    }

    fun updatePositionMappingField(colIndex: Int, fieldName: String) {
        val current = _uiState.value.positionMappingScreen ?: return
        val newMappings = current.columnMappings.toMutableMap()
        if (fieldName == "Skip") {
            newMappings.remove(colIndex)
        } else {
            val existingKey = newMappings.entries.find { it.value == fieldName }?.key
            if (existingKey != null) newMappings.remove(existingKey)
            newMappings[colIndex] = fieldName
        }
        _uiState.value = _uiState.value.copy(
            positionMappingScreen = current.copy(columnMappings = newMappings)
        )
    }

    fun savePositionMapping() {
        val screen = _uiState.value.positionMappingScreen ?: return
        viewModelScope.launch {
            val mappingsJson = serializeMappings(screen.columnMappings, screen.csvHeaders)
            csvMappingDao.upsertMapping(
                CsvImportMappingEntity(
                    importType = CsvImportType.Position.name,
                    mappingsJson = mappingsJson,
                    dateFormatJson = ""
                )
            )
            _uiState.value = _uiState.value.copy(
                positionMappingScreen = null,
                message = "Mapping saved for Position Details."
            )
        }
    }

    fun savePositionMappingAsNamed(name: String) {
        val screen = _uiState.value.positionMappingScreen ?: return
        viewModelScope.launch {
            val mappingsJson = serializeMappings(screen.columnMappings, screen.csvHeaders)
            csvMappingDao.insertNamedMapping(
                NamedCsvMappingEntity(
                    name = name,
                    importType = CsvImportType.Position.name,
                    mappingsJson = mappingsJson,
                    dateFormatJson = ""
                )
            )
            // Also save as the active mapping
            csvMappingDao.upsertMapping(
                CsvImportMappingEntity(
                    importType = CsvImportType.Position.name,
                    mappingsJson = mappingsJson,
                    dateFormatJson = ""
                )
            )
            val namedMappings = csvMappingDao.getNamedMappings(CsvImportType.Position.name)
            _uiState.value = _uiState.value.copy(
                positionMappingScreen = screen.copy(savedMappings = namedMappings),
                message = "Mapping \"$name\" saved."
            )
        }
    }

    fun loadNamedMapping(mappingId: Long) {
        val screen = _uiState.value.positionMappingScreen ?: return
        viewModelScope.launch {
            val named = csvMappingDao.getNamedMappingById(mappingId) ?: return@launch
            val mappings = deserializeMappings(named.mappingsJson, screen.csvHeaders)
            _uiState.value = _uiState.value.copy(
                positionMappingScreen = screen.copy(
                    columnMappings = mappings,
                    hasSavedMapping = true
                ),
                message = "Loaded mapping \"${named.name}\"."
            )
        }
    }

    fun deleteNamedMapping(mappingId: Long) {
        val screen = _uiState.value.positionMappingScreen ?: return
        viewModelScope.launch {
            csvMappingDao.deleteNamedMapping(mappingId)
            val namedMappings = csvMappingDao.getNamedMappings(CsvImportType.Position.name)
            _uiState.value = _uiState.value.copy(
                positionMappingScreen = screen.copy(savedMappings = namedMappings)
            )
        }
    }

    fun dismissPositionMappingScreen() {
        _uiState.value = _uiState.value.copy(positionMappingScreen = null)
    }

    fun dismissPositionImportResult() {
        _uiState.value = _uiState.value.copy(positionImportResult = null)
    }

    // --- Position Import with Mapping Selection ---

    fun showMappingSelection(fileUri: Uri) {
        viewModelScope.launch {
            val namedMappings = csvMappingDao.getNamedMappings(CsvImportType.Position.name)
            _uiState.value = _uiState.value.copy(
                showMappingSelectionDialog = true,
                savedPositionMappings = namedMappings,
                pendingImportFileUri = fileUri
            )
        }
    }

    fun dismissMappingSelection() {
        _uiState.value = _uiState.value.copy(
            showMappingSelectionDialog = false,
            pendingImportFileUri = null
        )
    }

    fun startPositionImportWithMapping(mappingId: Long?, accountId: Long) {
        val fileUri = _uiState.value.pendingImportFileUri ?: return
        _uiState.value = _uiState.value.copy(
            showMappingSelectionDialog = false,
            pendingImportFileUri = null
        )
        viewModelScope.launch {
            val mapping = if (mappingId != null) {
                val named = csvMappingDao.getNamedMappingById(mappingId)
                if (named != null) {
                    CsvImportMappingEntity(
                        importType = CsvImportType.Position.name,
                        mappingsJson = named.mappingsJson,
                        dateFormatJson = named.dateFormatJson
                    )
                } else null
            } else {
                csvMappingDao.getMapping(CsvImportType.Position.name)
            }

            if (mapping == null) {
                _uiState.value = _uiState.value.copy(
                    message = "No mapping found. Please define mapping first."
                )
                return@launch
            }

            startPositionImportWithLog(fileUri, mapping, accountId)
        }
    }

    private suspend fun startPositionImportWithLog(fileUri: Uri, mapping: CsvImportMappingEntity, accountId: Long) {
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
            val mappings = deserializeMappings(mapping.mappingsJson, headers)
            val total = rows.size

            _uiState.value = _uiState.value.copy(
                csvImport = CsvImportState(
                    importType = CsvImportType.Position,
                    isImporting = true,
                    importTotal = total,
                    importCurrent = 0,
                    importProgress = 0f
                )
            )

            val logEntries = mutableListOf<ImportLogEntry>()
            var imported = 0
            var updated = 0
            var skipped = 0

            for (row in rows) {
                val fieldValues = mutableMapOf<String, String>()
                for ((colIndex, fieldName) in mappings) {
                    if (colIndex < row.size) {
                        fieldValues[fieldName] = row[colIndex].trim()
                    }
                }

                try {
                    val rawTicker = fieldValues["ticker"]?.trim()?.uppercase()
                    val ticker = rawTicker?.split("\\s+-\\s+".toRegex())?.firstOrNull()?.trim()
                    if (ticker.isNullOrBlank()) {
                        skipped++
                        logEntries.add(ImportLogEntry(
                            ticker = rawTicker ?: "UNKNOWN",
                            status = ImportStatus.SKIPPED,
                            details = "Missing ticker"
                        ))
                        continue
                    }

                    val existing = itemDao.getItemByTicker(ticker)
                    val changedFields = mutableListOf<String>()

                    val newPrice = parseNumeric(fieldValues["currentPrice"])
                    val newQty = parseNumeric(fieldValues["quantity"])
                    val newCost = parseNumeric(fieldValues["cost"])
                    val newName = fieldValues["name"]?.ifBlank { null }
                    val newValue = parseNumeric(fieldValues["value"])
                    val newDayGL = parseNumeric(fieldValues["dayGainLoss"])
                    val newTotalGL = parseNumeric(fieldValues["totalGainLoss"])

                    if (existing != null) {
                        if (newPrice != null && newPrice != existing.currentPrice) changedFields.add("price: ${existing.currentPrice} → $newPrice")
                        if (newQty != null && newQty != existing.quantity) changedFields.add("qty: ${existing.quantity} → $newQty")
                        if (newCost != null && newCost != existing.cost) changedFields.add("cost: ${existing.cost} → $newCost")
                        if (newName != null && newName != existing.name) changedFields.add("name: ${existing.name} → $newName")
                        if (newValue != null && newValue != existing.value) changedFields.add("value: ${existing.value} → $newValue")
                        if (newDayGL != null && newDayGL != existing.dayGainLoss) changedFields.add("dayG/L: ${existing.dayGainLoss} → $newDayGL")
                        if (newTotalGL != null && newTotalGL != existing.totalGainLoss) changedFields.add("totalG/L: ${existing.totalGainLoss} → $newTotalGL")
                    }

                    val item = InvestmentItemEntity(
                        ticker = ticker,
                        name = newName ?: existing?.name ?: ticker,
                        type = fieldValues["type"]?.let {
                            runCatching { InvestmentType.valueOf(it) }.getOrNull()
                        } ?: existing?.type ?: InvestmentType.Stock,
                        currentPrice = newPrice ?: existing?.currentPrice ?: 0.0,
                        quantity = newQty ?: existing?.quantity ?: 0.0,
                        cost = newCost ?: existing?.cost ?: 0.0,
                        dayGainLoss = newDayGL ?: existing?.dayGainLoss ?: 0.0,
                        totalGainLoss = newTotalGL ?: existing?.totalGainLoss ?: 0.0,
                        value = newValue ?: existing?.value ?: 0.0,
                        dayHigh = existing?.dayHigh ?: 0.0,
                        dayLow = existing?.dayLow ?: 0.0
                    )
                    itemDao.upsertItem(item)

                    if (existing != null) {
                        updated++
                        logEntries.add(ImportLogEntry(
                            ticker = ticker,
                            status = ImportStatus.UPDATED,
                            details = if (changedFields.isEmpty()) "No changes" else changedFields.joinToString("; ")
                        ))
                    } else {
                        imported++
                        logEntries.add(ImportLogEntry(
                            ticker = ticker,
                            status = ImportStatus.IMPORTED,
                            details = "New position: price=$newPrice, qty=$newQty, cost=$newCost"
                        ))
                    }
                } catch (e: Exception) {
                    skipped++
                    val rawTicker = fieldValues["ticker"]?.trim()?.uppercase() ?: "UNKNOWN"
                    logEntries.add(ImportLogEntry(
                        ticker = rawTicker,
                        status = ImportStatus.SKIPPED,
                        details = e.message ?: "Unknown error"
                    ))
                }

                _uiState.value = _uiState.value.copy(
                    csvImport = _uiState.value.csvImport?.copy(
                        importCurrent = imported + updated + skipped,
                        importProgress = (imported + updated + skipped).toFloat() / total
                    )
                )
            }

            _uiState.value = _uiState.value.copy(
                csvImport = null,
                positionImportResult = PositionImportResult(
                    entries = logEntries,
                    totalImported = imported,
                    totalUpdated = updated,
                    totalSkipped = skipped
                )
            )
            com.investhelp.app.AppLog.log("Position import: $imported new, $updated updated, $skipped skipped")
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                csvImport = null,
                message = "Import failed: ${e.message}"
            )
        }
    }

    private fun autoMapHeaders(headers: List<String>, importType: CsvImportType): Map<Int, String> {
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
        return autoMappings
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
                            CsvImportType.Transaction -> importTransactionRow(fieldValues, fieldDateFormats)
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
        dateFormats: Map<String, String>
    ) {
        val rawTicker = fields["ticker"]?.trim()?.uppercase()
        val ticker = rawTicker?.split("\\s+-\\s+".toRegex())?.firstOrNull()?.trim()
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

        val totalAmount = parseNumeric(fields["totalAmount"]) ?: 0.0
        val note = fields["note"]?.trim() ?: ""

        transactionDao.insertTransaction(
            InvestmentTransactionEntity(
                date = date, time = time, action = action,
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
        val rawTicker = fields["ticker"]?.trim()?.uppercase()
        val ticker = rawTicker?.split("\\s+-\\s+".toRegex())?.firstOrNull()?.trim()
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
                date = date,
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

    // Definitions
    val definitions: StateFlow<List<DefinitionEntity>> =
        definitionDao.getAllDefinitions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addDefinition(name: String, description: String) {
        viewModelScope.launch {
            definitionDao.insertDefinition(DefinitionEntity(name = name, description = description))
        }
    }

    fun updateDefinition(definition: DefinitionEntity) {
        viewModelScope.launch {
            definitionDao.updateDefinition(definition)
        }
    }

    fun deleteDefinition(definition: DefinitionEntity) {
        viewModelScope.launch {
            definitionDao.deleteDefinition(definition)
        }
    }
}
