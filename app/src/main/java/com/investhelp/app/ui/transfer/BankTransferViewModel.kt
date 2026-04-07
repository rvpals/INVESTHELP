package com.investhelp.app.ui.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.local.entity.BankTransferEntity
import com.investhelp.app.data.local.entity.InvestmentAccountEntity
import com.investhelp.app.data.repository.AccountRepository
import com.investhelp.app.data.repository.BankTransferRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class BankTransferViewModel @Inject constructor(
    private val transferRepository: BankTransferRepository,
    accountRepository: AccountRepository
) : ViewModel() {

    val allTransfers: StateFlow<List<BankTransferEntity>> =
        transferRepository.getAllTransfers()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAccounts: StateFlow<List<InvestmentAccountEntity>> =
        accountRepository.getAllAccounts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTransfer = MutableStateFlow<BankTransferEntity?>(null)
    val selectedTransfer: StateFlow<BankTransferEntity?> = _selectedTransfer.asStateFlow()

    fun loadTransfer(transferId: Long) {
        viewModelScope.launch {
            transferRepository.getTransferById(transferId).collect { transfer ->
                _selectedTransfer.value = transfer
            }
        }
    }

    fun saveTransfer(
        date: LocalDate,
        amount: Double,
        accountId: Long,
        note: String,
        existingId: Long?
    ) {
        viewModelScope.launch {
            val transfer = BankTransferEntity(
                id = existingId ?: 0,
                date = date,
                amount = amount,
                accountId = accountId,
                note = note
            )
            if (existingId != null) {
                transferRepository.updateTransfer(transfer)
            } else {
                transferRepository.insertTransfer(transfer)
            }
        }
    }

    fun deleteTransfer(transfer: BankTransferEntity) {
        viewModelScope.launch {
            transferRepository.deleteTransfer(transfer)
        }
    }
}
