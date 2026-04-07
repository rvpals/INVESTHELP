package com.investhelp.app.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.local.entity.InvestmentAccountEntity
import com.investhelp.app.data.local.entity.InvestmentTransactionEntity
import com.investhelp.app.data.repository.AccountRepository
import com.investhelp.app.data.repository.TransactionRepository
import com.investhelp.app.model.TransactionAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    accountRepository: AccountRepository
) : ViewModel() {

    val allTransactions: StateFlow<List<InvestmentTransactionEntity>> =
        transactionRepository.getAllTransactions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAccounts: StateFlow<List<InvestmentAccountEntity>> =
        accountRepository.getAllAccounts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTransaction = MutableStateFlow<InvestmentTransactionEntity?>(null)
    val selectedTransaction: StateFlow<InvestmentTransactionEntity?> = _selectedTransaction.asStateFlow()

    fun loadTransaction(transactionId: Long) {
        viewModelScope.launch {
            transactionRepository.getTransactionById(transactionId).collect { transaction ->
                _selectedTransaction.value = transaction
            }
        }
    }

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
        }
    }

    fun deleteTransaction(transaction: InvestmentTransactionEntity) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }
}
