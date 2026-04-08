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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class SettingsUiState(
    val backupFolderUri: Uri? = null,
    val backupFolderName: String? = null,
    val message: String? = null,
    val isExporting: Boolean = false,
    val isRestoring: Boolean = false,
    val autoUpdateShares: Boolean = false
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
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            autoUpdateShares = prefs.getBoolean(KEY_AUTO_UPDATE_SHARES, false)
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val json = Json { prettyPrint = true }

    fun setAutoUpdateShares(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_UPDATE_SHARES, enabled).apply()
        _uiState.value = _uiState.value.copy(autoUpdateShares = enabled)
    }

    fun setBackupFolder(uri: Uri) {
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
                            value = it.value
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
                                value = i.value
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
}
