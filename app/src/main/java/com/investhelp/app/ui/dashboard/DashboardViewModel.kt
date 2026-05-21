package com.investhelp.app.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.local.entity.ChangeHistoryEntity
import com.investhelp.app.data.local.entity.WatchListItemEntity
import com.investhelp.app.data.remote.StockPriceService
import com.investhelp.app.data.repository.AccountRepository
import com.investhelp.app.data.repository.ChangeHistoryRepository
import com.investhelp.app.data.repository.InvestmentItemRepository
import com.investhelp.app.data.repository.WatchListRepository
import kotlinx.coroutines.flow.Flow
import com.investhelp.app.model.AccountWithValue
import com.investhelp.app.model.InvestmentType
import com.investhelp.app.ui.settings.SettingsViewModel
import com.investhelp.app.AppLog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class DailyGlanceItem(
    val ticker: String,
    val name: String,
    val dayGainLoss: Double,
    val dayGainLossPercent: Double,
    val dayGainLossPerShare: Double = 0.0
)

data class MarketIndexQuote(
    val symbol: String,
    val label: String,
    val price: Double = 0.0,
    val change: Double = 0.0,
    val changePercent: Double = 0.0
)

data class OverallDailyByType(
    val type: InvestmentType,
    val dayChange: Double,
    val dayChangePercent: Double
)

data class DashboardUiState(
    val accounts: List<AccountWithValue> = emptyList(),
    val totalPortfolioValue: Double = 0.0,
    val totalDayGainLoss: Double = 0.0,
    val totalCost: Double = 0.0,
    val marketIndices: List<MarketIndexQuote> = emptyList(),
    val topGainers: List<DailyGlanceItem> = emptyList(),
    val topLosers: List<DailyGlanceItem> = emptyList(),
    val overallDailyByType: List<OverallDailyByType> = emptyList()
)

data class DashboardWatchList(
    val id: Long,
    val name: String,
    val items: List<WatchListItemEntity>
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    accountRepository: AccountRepository,
    private val itemRepository: InvestmentItemRepository,
    private val stockPriceService: StockPriceService,
    private val changeHistoryRepository: ChangeHistoryRepository,
    private val watchListRepository: WatchListRepository
) : ViewModel() {

    private val prefs = context.getSharedPreferences(
        SettingsViewModel.PREFS_NAME, Context.MODE_PRIVATE
    )

    companion object {
        const val KEY_PIN_MARKET_INDICES = "pin_card_market_indices"
        const val KEY_PIN_DAILY_GLANCE = "pin_card_daily_glance"
        const val KEY_PIN_POSITION_DETAILS = "pin_card_position_details"
        const val KEY_PIN_PORTFOLIO_SUMMARY = "pin_card_portfolio_summary"
        const val KEY_PIN_WATCH_LIST = "pin_card_watch_list"
    }

    val changeHistoryRecords: Flow<List<ChangeHistoryEntity>> = changeHistoryRepository.getAllRecords()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    data class RefreshStatus(
        val ticker: String = "",
        val price: Double = 0.0,
        val changeAmount: Double = 0.0,
        val changePercent: Double = 0.0
    )

    private val _refreshStatus = MutableStateFlow<RefreshStatus?>(null)
    val refreshStatus: StateFlow<RefreshStatus?> = _refreshStatus.asStateFlow()

    private val _lastRefreshedAt = MutableStateFlow<LocalDateTime?>(
        prefs.getLong("last_refreshed_at", -1L).let { millis ->
            if (millis > 0) LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(millis), java.time.ZoneId.systemDefault()
            ) else null
        }
    )
    val lastRefreshedAt: StateFlow<LocalDateTime?> = _lastRefreshedAt.asStateFlow()

    private val _pinStates = MutableStateFlow(
        mapOf(
            KEY_PIN_MARKET_INDICES to prefs.getBoolean(KEY_PIN_MARKET_INDICES, false),
            KEY_PIN_DAILY_GLANCE to prefs.getBoolean(KEY_PIN_DAILY_GLANCE, false),
            KEY_PIN_POSITION_DETAILS to prefs.getBoolean(KEY_PIN_POSITION_DETAILS, false),
            KEY_PIN_PORTFOLIO_SUMMARY to prefs.getBoolean(KEY_PIN_PORTFOLIO_SUMMARY, true),
            KEY_PIN_WATCH_LIST to prefs.getBoolean(KEY_PIN_WATCH_LIST, false)
        )
    )
    val pinStates: StateFlow<Map<String, Boolean>> = _pinStates.asStateFlow()

    private val _watchListCardVisible = MutableStateFlow(
        prefs.getBoolean(SettingsViewModel.KEY_CARD_VISIBLE_WATCH_LIST, true)
    )
    val watchListCardVisible: StateFlow<Boolean> = _watchListCardVisible.asStateFlow()

    private val _dashboardCardOrder = MutableStateFlow(
        prefs.getString(SettingsViewModel.KEY_DASHBOARD_CARD_ORDER, null)
            ?.split(",")?.filter { it.isNotBlank() }
            ?: SettingsViewModel.DEFAULT_CARD_ORDER
    )
    val dashboardCardOrder: StateFlow<List<String>> = _dashboardCardOrder.asStateFlow()

    private val _dashboardWatchLists = MutableStateFlow<List<DashboardWatchList>>(emptyList())
    val dashboardWatchLists: StateFlow<List<DashboardWatchList>> = _dashboardWatchLists.asStateFlow()

    fun setWatchListCardVisible(visible: Boolean) {
        prefs.edit().putBoolean(SettingsViewModel.KEY_CARD_VISIBLE_WATCH_LIST, visible).apply()
        _watchListCardVisible.value = visible
    }

    fun setDashboardCardOrder(order: List<String>) {
        prefs.edit().putString(SettingsViewModel.KEY_DASHBOARD_CARD_ORDER, order.joinToString(",")).apply()
        _dashboardCardOrder.value = order
    }

    fun moveDashboardCard(cardKey: String, direction: Int) {
        val current = _dashboardCardOrder.value.toMutableList()
        val index = current.indexOf(cardKey)
        if (index < 0) return
        val newIndex = index + direction
        if (newIndex < 0 || newIndex >= current.size) return
        current[index] = current[newIndex].also { current[newIndex] = current[index] }
        setDashboardCardOrder(current)
    }

    fun setPinState(key: String, pinned: Boolean) {
        prefs.edit().putBoolean(key, pinned).apply()
        _pinStates.value = _pinStates.value.toMutableMap().apply { put(key, pinned) }
    }

    fun reorderMarketIndices(newOrder: List<String>) {
        prefs.edit().putString(SettingsViewModel.KEY_MARKET_INDICES_ORDER, newOrder.joinToString(",")).apply()
        _marketIndices.value = newOrder.mapNotNull { symbol ->
            _marketIndices.value.find { it.symbol == symbol }
        }
    }

    private val _marketIndices = MutableStateFlow<List<MarketIndexQuote>>(emptyList())

    init {
        refreshMarketIndices()
        fetchWatchLists()
    }

    private fun fetchWatchLists() {
        viewModelScope.launch {
            watchListRepository.getAllWatchLists().collect { lists ->
                val dashboardLists = lists.take(2).map { watchList ->
                    val items = watchListRepository.getItemsByWatchList(watchList.id).first()
                    DashboardWatchList(
                        id = watchList.id,
                        name = watchList.name,
                        items = items.take(5)
                    )
                }
                _dashboardWatchLists.value = dashboardLists
            }
        }
    }

    fun refreshMarketIndices() {
        viewModelScope.launch {
            val enabledSymbols = prefs.getStringSet(
                SettingsViewModel.KEY_MARKET_INDICES, null
            ) ?: SettingsViewModel.DEFAULT_MARKET_INDICES

            val savedOrder = prefs.getString(SettingsViewModel.KEY_MARKET_INDICES_ORDER, null)
            val orderList = savedOrder?.split(",")?.filter { it.isNotBlank() }
            val orderMap = orderList?.withIndex()?.associate { it.value to it.index }

            val orderedSymbols = SettingsViewModel.AVAILABLE_MARKET_INDICES
                .filter { it.symbol in enabledSymbols }
                .let { list ->
                    if (orderMap != null) list.sortedBy { orderMap[it.symbol] ?: Int.MAX_VALUE }
                    else list
                }

            _marketIndices.value = orderedSymbols.map {
                MarketIndexQuote(symbol = it.symbol, label = it.label)
            }

            var successCount = 0
            var failCount = 0
            for (config in orderedSymbols) {
                try {
                    val quote = stockPriceService.fetchQuote(config.symbol)
                    val change = quote.price - quote.previousClose
                    val changePct = if (quote.previousClose != 0.0)
                        change / quote.previousClose * 100.0 else 0.0

                    _marketIndices.value = _marketIndices.value.map {
                        if (it.symbol == config.symbol) it.copy(
                            price = quote.price,
                            change = change,
                            changePercent = changePct
                        ) else it
                    }
                    successCount++
                } catch (e: Exception) {
                    failCount++
                    AppLog.log("Market index ${config.symbol} fetch failed: ${e.message}")
                }
            }
            AppLog.log("Market indices refreshed: $successCount ok" +
                    if (failCount > 0) ", $failCount failed" else "")
        }
    }

    fun refreshAllPrices() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                refreshMarketIndices()

                val allItems = itemRepository.getAllItems().first()
                var successCount = 0
                var failCount = 0
                for (item in allItems) {
                    try {
                        val quote = stockPriceService.fetchQuote(item.ticker)
                        val resolvedName = quote.shortName ?: item.name
                        val newValue = quote.price * item.quantity
                        val dayChange = (quote.price - quote.previousClose) * item.quantity
                        val priceChange = quote.price - quote.previousClose
                        val priceChangePct = if (quote.previousClose != 0.0)
                            priceChange / quote.previousClose * 100.0 else 0.0
                        _refreshStatus.value = RefreshStatus(
                            ticker = item.ticker,
                            price = quote.price,
                            changeAmount = priceChange,
                            changePercent = priceChangePct
                        )
                        itemRepository.upsertItem(
                            item.copy(
                                name = resolvedName,
                                currentPrice = quote.price,
                                value = newValue,
                                dayGainLoss = dayChange,
                                totalGainLoss = newValue - item.cost
                            )
                        )
                        if (item.logo == null) {
                            stockPriceService.fetchLogo(item.ticker)?.let { logo ->
                                itemRepository.updateLogoByTicker(item.ticker, logo)
                            }
                        }
                        successCount++
                    } catch (e: Exception) {
                        failCount++
                        AppLog.log("Price fetch ${item.ticker} failed: ${e.message}")
                    }
                }
                AppLog.log("Portfolio refresh: $successCount tickers ok" +
                        if (failCount > 0) ", $failCount failed" else "")

                val now = LocalDateTime.now()
                _lastRefreshedAt.value = now
                prefs.edit().putLong("last_refreshed_at",
                    now.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                ).apply()

                if (prefs.getBoolean(SettingsViewModel.KEY_AUTO_UPDATE_CHANGE_HISTORY, false)) {
                    recordChangeHistory()
                }
            } finally {
                _isRefreshing.value = false
                _refreshStatus.value = null
            }
        }
    }

    private suspend fun recordChangeHistory() {
        try {
            val allItems = itemRepository.getAllItems().first()
            val etfValue = allItems
                .filter { it.type == InvestmentType.ETF }
                .sumOf { it.value }
            val stockValue = allItems
                .filter { it.type == InvestmentType.Stock }
                .sumOf { it.value }
            val totalValue = allItems.sumOf { it.value }
            val dailyChangeEtf = allItems
                .filter { it.type == InvestmentType.ETF }
                .sumOf { it.dayGainLoss }
            val dailyChangeStock = allItems
                .filter { it.type == InvestmentType.Stock }
                .sumOf { it.dayGainLoss }
            val dailyChangeTotal = allItems.sumOf { it.dayGainLoss }
            val today = LocalDate.now()

            val existing = changeHistoryRepository.getRecordByDate(today)
            changeHistoryRepository.upsertRecord(
                ChangeHistoryEntity(
                    id = existing?.id ?: 0,
                    date = today,
                    etfValue = etfValue,
                    stockValue = stockValue,
                    totalValue = totalValue,
                    dailyChangeEtf = dailyChangeEtf,
                    dailyChangeStock = dailyChangeStock,
                    dailyChangeTotal = dailyChangeTotal
                )
            )
            AppLog.log("Change history recorded: ETF=${"%.2f".format(etfValue)}, Stock=${"%.2f".format(stockValue)}, Total=${"%.2f".format(totalValue)}, DailyΔ ETF=${"%.2f".format(dailyChangeEtf)}, Stock=${"%.2f".format(dailyChangeStock)}, Total=${"%.2f".format(dailyChangeTotal)}")
        } catch (e: Exception) {
            AppLog.log("Change history record failed: ${e.message}")
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        accountRepository.getAllAccountsWithValues(),
        itemRepository.getAllItems(),
        _marketIndices
    ) { accounts, items, indices ->
        val glanceItems = items.map { item ->
            val previousValue = item.value - item.dayGainLoss
            val pct = if (previousValue != 0.0) item.dayGainLoss / previousValue * 100.0 else 0.0
            val perShare = if (item.quantity != 0.0) item.dayGainLoss / item.quantity else 0.0
            DailyGlanceItem(
                ticker = item.ticker,
                name = item.name,
                dayGainLoss = item.dayGainLoss,
                dayGainLossPercent = pct,
                dayGainLossPerShare = perShare
            )
        }
        val topGainers = glanceItems
            .filter { it.dayGainLoss > 0 }
            .sortedByDescending { it.dayGainLoss }
            .take(5)
        val topLosers = glanceItems
            .filter { it.dayGainLoss < 0 }
            .sortedBy { it.dayGainLoss }
            .take(5)

        val overallByType = items
            .groupBy { it.type }
            .filter { it.key == InvestmentType.Stock || it.key == InvestmentType.ETF }
            .map { (type, list) ->
                val totalDayGL = list.sumOf { it.dayGainLoss }
                val totalValue = list.sumOf { it.value }
                val previousValue = totalValue - totalDayGL
                val pct = if (previousValue != 0.0) totalDayGL / previousValue * 100.0 else 0.0
                OverallDailyByType(type = type, dayChange = totalDayGL, dayChangePercent = pct)
            }
            .sortedBy { it.type.name }

        DashboardUiState(
            accounts = accounts,
            totalPortfolioValue = items.sumOf { it.value },
            totalDayGainLoss = items.sumOf { it.dayGainLoss },
            totalCost = items.sumOf { it.cost },
            marketIndices = indices,
            topGainers = topGainers,
            topLosers = topLosers,
            overallDailyByType = overallByType
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
