package com.investhelp.app.ui.transaction

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.local.dao.InvestmentItemDao
import com.investhelp.app.data.local.entity.InvestmentAccountEntity
import com.investhelp.app.data.local.entity.InvestmentItemEntity
import com.investhelp.app.data.local.entity.InvestmentTransactionEntity
import com.investhelp.app.data.repository.AccountRepository
import com.investhelp.app.data.repository.TransactionRepository
import com.investhelp.app.model.InvestmentType
import com.investhelp.app.model.TransactionAction
import com.investhelp.app.ui.settings.SettingsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val itemDao: InvestmentItemDao,
    accountRepository: AccountRepository
) : ViewModel() {

    val allTransactions: StateFlow<List<InvestmentTransactionEntity>> =
        transactionRepository.getAllTransactions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAccounts: StateFlow<List<InvestmentAccountEntity>> =
        accountRepository.getAllAccounts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentPrices: StateFlow<Map<String, Double>> =
        itemDao.getAllItems()
            .map { items ->
                items.associate { it.ticker to it.currentPrice }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _selectedTransaction = MutableStateFlow<InvestmentTransactionEntity?>(null)
    val selectedTransaction: StateFlow<InvestmentTransactionEntity?> = _selectedTransaction.asStateFlow()

    fun loadTransaction(transactionId: Long) {
        viewModelScope.launch {
            transactionRepository.getTransactionById(transactionId).collect { transaction ->
                _selectedTransaction.value = transaction
            }
        }
    }

    private val prefs = context.getSharedPreferences(
        SettingsViewModel.PREFS_NAME, Context.MODE_PRIVATE
    )

    fun saveTransaction(
        date: LocalDate,
        time: LocalTime?,
        action: TransactionAction,
        accountId: Long,
        ticker: String,
        numberOfShares: Double,
        pricePerShare: Double,
        totalAmount: Double,
        note: String,
        existingId: Long?
    ) {
        viewModelScope.launch {
            val transaction = InvestmentTransactionEntity(
                id = existingId ?: 0,
                date = date,
                time = time,
                action = action,
                accountId = accountId,
                ticker = ticker,
                numberOfShares = numberOfShares,
                pricePerShare = pricePerShare,
                totalAmount = totalAmount,
                note = note
            )
            if (existingId != null) {
                transactionRepository.updateTransaction(transaction)
            } else {
                transactionRepository.insertTransaction(transaction)
            }

            // Auto-create investment item if none exists for this ticker
            if (itemDao.getItemByTicker(ticker) == null) {
                itemDao.upsertItem(
                    InvestmentItemEntity(
                        ticker = ticker,
                        name = ticker,
                        type = InvestmentType.Stock,
                        currentPrice = pricePerShare,
                        quantity = 0.0,
                        cost = 0.0,
                        dayGainLoss = 0.0,
                        totalGainLoss = 0.0,
                        value = 0.0,
                        dayHigh = 0.0,
                        dayLow = 0.0
                    )
                )
            }

            // Auto-update position if enabled
            if (prefs.getBoolean(SettingsViewModel.KEY_AUTO_UPDATE_SHARES, false)) {
                val delta = when (action) {
                    TransactionAction.Buy -> numberOfShares
                    TransactionAction.Sell -> -numberOfShares
                }

                val existing = itemDao.getItemByTicker(ticker)
                if (existing != null) {
                    val newQuantity = (existing.quantity + delta).coerceAtLeast(0.0)
                    itemDao.upsertItem(existing.copy(quantity = newQuantity))
                }
            }
        }
    }

    fun deleteTransaction(transaction: InvestmentTransactionEntity) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }

    fun deleteTransactions(transactions: List<InvestmentTransactionEntity>) {
        viewModelScope.launch {
            transactionRepository.deleteTransactions(transactions)
        }
    }
}
