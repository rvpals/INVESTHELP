package com.investhelp.app.ui.performance

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.local.entity.AccountPerformanceEntity
import com.investhelp.app.data.local.entity.InvestmentAccountEntity
import com.investhelp.app.data.repository.AccountPerformanceRepository
import com.investhelp.app.data.repository.AccountRepository
import com.investhelp.app.ui.settings.SettingsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AccountPerformanceViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val performanceRepository: AccountPerformanceRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(SettingsViewModel.PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val KEY_PIN_ADD_RECORD = "pin_card_perf_add_record"
        const val KEY_PIN_CHART = "pin_card_perf_chart"
        const val KEY_PIN_RECORDS = "pin_card_perf_records"
        const val KEY_RECORD_FILTER_ACCOUNT_IDS = "perf_record_filter_account_ids"
        const val KEY_RECORD_SORT_FIELD = "perf_record_sort_field"
        const val KEY_RECORD_SORT_ASC = "perf_record_sort_asc"
    }

    private val _pinStates = MutableStateFlow(
        mapOf(
            KEY_PIN_ADD_RECORD to prefs.getBoolean(KEY_PIN_ADD_RECORD, false),
            KEY_PIN_CHART to prefs.getBoolean(KEY_PIN_CHART, true),
            KEY_PIN_RECORDS to prefs.getBoolean(KEY_PIN_RECORDS, true),
        )
    )
    val pinStates: StateFlow<Map<String, Boolean>> = _pinStates.asStateFlow()

    fun setPinState(key: String, pinned: Boolean) {
        prefs.edit().putBoolean(key, pinned).apply()
        _pinStates.value = _pinStates.value.toMutableMap().apply { put(key, pinned) }
    }

    private val savedFilterIds: Set<String>? = prefs.getStringSet(KEY_RECORD_FILTER_ACCOUNT_IDS, null)

    private val _recordFilterAccountIds = MutableStateFlow<Set<Long>?>(
        savedFilterIds?.mapNotNull { it.toLongOrNull() }?.toSet()
    )
    val recordFilterAccountIds: StateFlow<Set<Long>?> = _recordFilterAccountIds.asStateFlow()

    private val _recordSortField = MutableStateFlow(prefs.getString(KEY_RECORD_SORT_FIELD, "Date") ?: "Date")
    val recordSortField: StateFlow<String> = _recordSortField.asStateFlow()

    private val _recordSortAsc = MutableStateFlow(prefs.getBoolean(KEY_RECORD_SORT_ASC, false))
    val recordSortAsc: StateFlow<Boolean> = _recordSortAsc.asStateFlow()

    fun setRecordFilterAccountIds(ids: Set<Long>) {
        _recordFilterAccountIds.value = ids
        prefs.edit().putStringSet(KEY_RECORD_FILTER_ACCOUNT_IDS, ids.map { it.toString() }.toSet()).apply()
    }

    fun setRecordSortField(field: String) {
        _recordSortField.value = field
        prefs.edit().putString(KEY_RECORD_SORT_FIELD, field).apply()
    }

    fun setRecordSortAsc(asc: Boolean) {
        _recordSortAsc.value = asc
        prefs.edit().putBoolean(KEY_RECORD_SORT_ASC, asc).apply()
    }

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

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    fun clearSaveError() { _saveError.value = null }

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

    fun pullValueFromApp() {
        viewModelScope.launch {
            val value = accountRepository.computeTotalPortfolioValue()
            _pulledValue.value = value
        }
    }

    fun clearPulledValue() {
        _pulledValue.value = null
    }

    fun saveRecord(accountId: Long, totalValue: Double, note: String = "", date: LocalDate = LocalDate.now()): Boolean {
        viewModelScope.launch {
            if (performanceRepository.existsRecord(accountId, date)) {
                _saveError.value = "A record already exists for this account on ${date}"
                return@launch
            }
            val record = AccountPerformanceEntity(
                accountId = accountId,
                totalValue = totalValue,
                date = date,
                note = note.trim()
            )
            performanceRepository.insertRecord(record)
        }
        return true
    }

    fun updateRecord(record: AccountPerformanceEntity) {
        viewModelScope.launch {
            performanceRepository.updateRecord(record)
        }
    }

    fun deleteRecord(record: AccountPerformanceEntity) {
        viewModelScope.launch {
            performanceRepository.deleteRecord(record)
        }
    }
}
