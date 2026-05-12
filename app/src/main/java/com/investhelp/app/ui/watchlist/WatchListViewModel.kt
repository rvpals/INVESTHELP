package com.investhelp.app.ui.watchlist

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.AppLog
import com.investhelp.app.ReminderReceiver
import com.investhelp.app.data.local.entity.WatchListEntity
import com.investhelp.app.data.local.entity.WatchListItemEntity
import com.investhelp.app.data.remote.StockPriceService
import com.investhelp.app.data.repository.WatchListRepository
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
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

data class WatchListItemUi(
    val entity: WatchListItemEntity,
    val currentPrice: Double = 0.0,
    val changeAmount: Double = 0.0,
    val changePercent: Double = 0.0
)

@HiltViewModel
class WatchListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: WatchListRepository,
    private val stockPriceService: StockPriceService
) : ViewModel() {

    private val prefs = context.getSharedPreferences(
        SettingsViewModel.PREFS_NAME, Context.MODE_PRIVATE
    )

    val watchLists: StateFlow<List<WatchListEntity>> =
        repository.getAllWatchLists()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedWatchListId = MutableStateFlow<Long?>(null)
    val selectedWatchListId: StateFlow<Long?> = _selectedWatchListId.asStateFlow()

    private val _items = MutableStateFlow<List<WatchListItemUi>>(emptyList())
    val items: StateFlow<List<WatchListItemUi>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _fetchedPrice = MutableStateFlow<Double?>(null)
    val fetchedPrice: StateFlow<Double?> = _fetchedPrice.asStateFlow()

    private var itemsJob: Job? = null

    fun warnBeforeDelete(): Boolean =
        prefs.getBoolean(SettingsViewModel.KEY_WARN_BEFORE_DELETE, true)

    fun selectWatchList(id: Long) {
        _selectedWatchListId.value = id
        loadItems(id)
    }

    private fun loadItems(watchListId: Long) {
        itemsJob?.cancel()
        itemsJob = viewModelScope.launch {
            repository.getItemsByWatchList(watchListId).collect { entities ->
                _items.value = entities.map { entity ->
                    WatchListItemUi(entity = entity)
                }
                refreshPrices()
            }
        }
    }

    fun refreshPrices() {
        viewModelScope.launch {
            _isLoading.value = true
            val updated = _items.value.map { item ->
                try {
                    val quote = stockPriceService.fetchQuote(item.entity.ticker)
                    val currentPrice = quote.price
                    val costBasis = item.entity.priceWhenAdded * item.entity.shares
                    val currentValue = currentPrice * item.entity.shares
                    val changeAmt = currentValue - costBasis
                    val changePct = if (costBasis != 0.0) changeAmt / costBasis * 100.0 else 0.0
                    item.copy(
                        currentPrice = currentPrice,
                        changeAmount = changeAmt,
                        changePercent = changePct
                    )
                } catch (e: Exception) {
                    AppLog.log("WatchList price fetch ${item.entity.ticker} failed: ${e.message}")
                    item
                }
            }
            _items.value = updated
            _isLoading.value = false
        }
    }

    fun createWatchList(name: String) {
        viewModelScope.launch {
            val id = repository.insertWatchList(WatchListEntity(name = name))
            _selectedWatchListId.value = id
            loadItems(id)
        }
    }

    fun renameWatchList(watchList: WatchListEntity, newName: String) {
        viewModelScope.launch {
            repository.updateWatchList(watchList.copy(name = newName))
        }
    }

    fun deleteWatchList(watchList: WatchListEntity) {
        viewModelScope.launch {
            repository.deleteWatchList(watchList)
            if (_selectedWatchListId.value == watchList.id) {
                _selectedWatchListId.value = null
                _items.value = emptyList()
            }
        }
    }

    fun fetchPrice(ticker: String) {
        _fetchedPrice.value = null
        viewModelScope.launch {
            try {
                val quote = stockPriceService.fetchQuote(ticker)
                _fetchedPrice.value = quote.price
            } catch (e: Exception) {
                AppLog.log("WatchList fetch price for $ticker failed: ${e.message}")
                _fetchedPrice.value = null
            }
        }
    }

    fun clearFetchedPrice() {
        _fetchedPrice.value = null
    }

    fun addItem(
        watchListId: Long,
        ticker: String,
        shares: Double,
        priceWhenAdded: Double,
        reminderDateTime: LocalDateTime? = null,
        reminderMessage: String? = null
    ) {
        viewModelScope.launch {
            val id = repository.insertItem(
                WatchListItemEntity(
                    watchListId = watchListId,
                    ticker = ticker.uppercase().trim(),
                    shares = shares,
                    priceWhenAdded = priceWhenAdded,
                    addedDate = LocalDate.now(),
                    reminderDateTime = reminderDateTime,
                    reminderMessage = reminderMessage
                )
            )
            if (reminderDateTime != null && reminderMessage != null) {
                scheduleReminder(id, ticker.uppercase().trim(), reminderDateTime, reminderMessage)
            }
        }
    }

    fun updateItemReminder(item: WatchListItemEntity, reminderDateTime: LocalDateTime?, reminderMessage: String?) {
        viewModelScope.launch {
            repository.updateItem(item.copy(
                reminderDateTime = reminderDateTime,
                reminderMessage = reminderMessage
            ))
            if (reminderDateTime != null && reminderMessage != null) {
                scheduleReminder(item.id, item.ticker, reminderDateTime, reminderMessage)
            } else {
                cancelReminder(item.id)
            }
        }
    }

    private fun scheduleReminder(itemId: Long, ticker: String, dateTime: LocalDateTime, message: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            AppLog.log("Cannot schedule exact alarm for $ticker — permission not granted")
            return
        }
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TICKER, ticker)
            putExtra(ReminderReceiver.EXTRA_MESSAGE, message)
            putExtra(ReminderReceiver.EXTRA_ITEM_ID, itemId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, itemId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerMs = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            AppLog.log("Reminder scheduled for $ticker at $dateTime")
        } catch (e: Exception) {
            AppLog.log("Failed to schedule reminder for $ticker: ${e.message}")
        }
    }

    private fun cancelReminder(itemId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, itemId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun deleteItem(item: WatchListItemEntity) {
        viewModelScope.launch {
            cancelReminder(item.id)
            repository.deleteItem(item)
        }
    }
}
