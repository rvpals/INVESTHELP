package com.investhelp.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.local.entity.AccountPerformanceEntity
import com.investhelp.app.data.local.entity.InvestmentAccountEntity
import com.investhelp.app.data.repository.AccountPerformanceRepository
import com.investhelp.app.data.repository.AccountRepository
import com.investhelp.app.model.AccountWithValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val performanceRepository: AccountPerformanceRepository
) : ViewModel() {

    val accountsWithValues: StateFlow<List<AccountWithValue>> =
        accountRepository.getAllAccountsWithValues()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPerformanceByAccount: StateFlow<Map<Long, List<AccountPerformanceEntity>>> =
        performanceRepository.getAllRecords()
            .map { records -> records.groupBy { it.accountId }.mapValues { (_, v) -> v.sortedBy { it.date } } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _selectedAccount = MutableStateFlow<InvestmentAccountEntity?>(null)
    val selectedAccount: StateFlow<InvestmentAccountEntity?> = _selectedAccount.asStateFlow()

    private val _performanceRecords = MutableStateFlow<List<AccountPerformanceEntity>>(emptyList())
    val performanceRecords: StateFlow<List<AccountPerformanceEntity>> = _performanceRecords.asStateFlow()

    private var performanceJob: Job? = null

    fun loadAccount(accountId: Long) {
        viewModelScope.launch {
            accountRepository.getAccountById(accountId).collect { account ->
                _selectedAccount.value = account
            }
        }
        performanceJob?.cancel()
        performanceJob = viewModelScope.launch {
            performanceRepository.getRecordsByAccount(accountId).collect { records ->
                _performanceRecords.value = records.sortedBy { it.date }
            }
        }
    }

    fun saveAccount(name: String, description: String, initialValue: Double, existingId: Long?) {
        viewModelScope.launch {
            if (existingId != null) {
                val existing = _selectedAccount.value
                val account = InvestmentAccountEntity(
                    id = existingId,
                    name = name,
                    description = description,
                    initialValue = initialValue,
                    lastUpdatedOn = existing?.lastUpdatedOn,
                    lastValue = existing?.lastValue
                )
                accountRepository.updateAccount(account)
            } else {
                val account = InvestmentAccountEntity(
                    id = 0,
                    name = name,
                    description = description,
                    initialValue = initialValue
                )
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
