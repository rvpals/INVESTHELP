package com.investhelp.app.ui.performance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.local.entity.AccountPerformanceEntity
import com.investhelp.app.data.local.entity.InvestmentAccountEntity
import com.investhelp.app.data.repository.AccountPerformanceRepository
import com.investhelp.app.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class AccountPerformanceViewModel @Inject constructor(
    private val performanceRepository: AccountPerformanceRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    val allRecords: StateFlow<List<AccountPerformanceEntity>> =
        performanceRepository.getAllRecords()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAccounts: StateFlow<List<InvestmentAccountEntity>> =
        accountRepository.getAllAccounts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _chartData = MutableStateFlow<Map<Long, List<AccountPerformanceEntity>>>(emptyMap())
    val chartData: StateFlow<Map<Long, List<AccountPerformanceEntity>>> = _chartData.asStateFlow()

    private var chartJob: Job? = null

    private val _pulledValue = MutableStateFlow<Double?>(null)
    val pulledValue: StateFlow<Double?> = _pulledValue.asStateFlow()

    fun loadChartData(accountIds: Set<Long>) {
        chartJob?.cancel()
        if (accountIds.isEmpty()) {
            _chartData.value = emptyMap()
            return
        }
        chartJob = viewModelScope.launch {
            performanceRepository.getRecordsByAccounts(accountIds.toList()).collect { records ->
                _chartData.value = records.groupBy { it.accountId }
            }
        }
    }

    fun pullValueFromApp(accountId: Long) {
        viewModelScope.launch {
            val value = accountRepository.computeCurrentValue(accountId)
            _pulledValue.value = value
        }
    }

    fun clearPulledValue() {
        _pulledValue.value = null
    }

    fun saveRecord(accountId: Long, totalValue: Double) {
        viewModelScope.launch {
            val record = AccountPerformanceEntity(
                accountId = accountId,
                totalValue = totalValue,
                dateTime = LocalDateTime.now()
            )
            performanceRepository.insertRecord(record)
        }
    }

    fun deleteRecord(record: AccountPerformanceEntity) {
        viewModelScope.launch {
            performanceRepository.deleteRecord(record)
        }
    }
}
