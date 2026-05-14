package com.investhelp.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.local.entity.InvestmentAccountEntity
import com.investhelp.app.data.repository.AccountRepository
import com.investhelp.app.model.AccountWithValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    val accountsWithValues: StateFlow<List<AccountWithValue>> =
        accountRepository.getAllAccountsWithValues()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedAccount = MutableStateFlow<InvestmentAccountEntity?>(null)
    val selectedAccount: StateFlow<InvestmentAccountEntity?> = _selectedAccount.asStateFlow()

    fun loadAccount(accountId: Long) {
        viewModelScope.launch {
            accountRepository.getAccountById(accountId).collect { account ->
                _selectedAccount.value = account
            }
        }
    }

    fun saveAccount(name: String, description: String, initialValue: Double, existingId: Long?) {
        viewModelScope.launch {
            val account = InvestmentAccountEntity(
                id = existingId ?: 0,
                name = name,
                description = description,
                initialValue = initialValue
            )
            if (existingId != null) {
                accountRepository.updateAccount(account)
            } else {
                accountRepository.insertAccount(account)
            }
        }
    }

    fun deleteAccount(account: InvestmentAccountEntity) {
        viewModelScope.launch {
            accountRepository.deleteAccount(account)
        }
    }
}
