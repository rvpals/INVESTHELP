package com.investhelp.app.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.local.entity.ChangeHistoryEntity
import com.investhelp.app.data.remote.StockPriceService
import com.investhelp.app.data.repository.AccountRepository
import com.investhelp.app.data.repository.ChangeHistoryRepository
import com.investhelp.app.data.repository.InvestmentItemRepository
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

data class TickerPosition(
    val ticker: String,
    val totalQuantity: Double,
    val totalValue: Double
)

data class PositionDetail(
    val ticker: String,
    val name: String,
    val totalShares: Double,
    val currentPrice: Double,
    val totalCost: Double,
    val totalValue: Double,
    val changeAmount: Double = 0.0,
    val changePercent: Double = 0.0,
    val logo: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PositionDetail) return false
        return ticker == other.ticker
    }

    override fun hashCode(): Int = ticker.hashCode()
}

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
    val positions: List<TickerPosition> = emptyList(),
    val marketIndices: List<MarketIndexQuote> = emptyList(),
    val topGainers: List<DailyGlanceItem> = emptyList(),
    val topLosers: List<DailyGlanceItem> = emptyList(),
    val overallDailyByType: List<OverallDailyByType> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    accountRepository: AccountRepository,
    private val itemRepository: InvestmentItemRepository,
    private val stockPriceService: StockPriceService,
    private val changeHistoryRepository: ChangeHistoryRepository
) : ViewModel() {

    private val prefs = context.getSharedPreferences(
        SettingsViewModel.PREFS_NAME, Context.MODE_PRIVATE
    )

    companion object {
        const val KEY_PIN_MARKET_INDICES = "pin_card_market_indices"
        const val KEY_PIN_POSITIONS = "pin_card_positions"
        const val KEY_PIN_DAILY_GLANCE = "pin_card_daily_glance"
        const val KEY_PIN_POSITION_DETAILS = "pin_card_position_details"
        const val KEY_PIN_PORTFOLIO_SUMMARY = "pin_card_portfolio_summary"
    }

    val changeHistoryRecords: Flow<List<ChangeHistoryEntity>> = changeHistoryRepository.getAllRecords()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

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
            KEY_PIN_POSITIONS to prefs.getBoolean(KEY_PIN_POSITIONS, false),
            KEY_PIN_DAILY_GLANCE to prefs.getBoolean(KEY_PIN_DAILY_GLANCE, false),
            KEY_PIN_POSITION_DETAILS to prefs.getBoolean(KEY_PIN_POSITION_DETAILS, false),
            KEY_PIN_PORTFOLIO_SUMMARY to prefs.getBoolean(KEY_PIN_PORTFOLIO_SUMMARY, true)
        )
    )
    val pinStates: StateFlow<Map<String, Boolean>> = _pinStates.asStateFlow()

    private val _positionDetails = MutableStateFlow<List<PositionDetail>>(emptyList())
    val positionDetails: StateFlow<List<PositionDetail>> = _positionDetails.asStateFlow()

    fun fetchPositionDetails() {
        viewModelScope.launch {
            val allItems = itemRepository.getAllItems().first()
            val details = allItems
                .filter { it.quantity > 0 }
                .map { item ->
                    val changeAmt = item.value - item.cost
                    val changePct = if (item.cost != 0.0) changeAmt / item.cost * 100.0 else 0.0
                    PositionDetail(
                        ticker = item.ticker, name = item.name,
                        totalShares = item.quantity, currentPrice = item.currentPrice,
                        totalCost = item.cost, totalValue = item.value,
                        changeAmount = changeAmt, changePercent = changePct,
                        logo = item.logo
                    )
                }
                .sortedByDescending { it.totalValue }
            _positionDetails.value = details
        }
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
        fetchPositionDetails()
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
            val today = LocalDate.now()

            val existing = changeHistoryRepository.getRecordByDate(today)
            changeHistoryRepository.upsertRecord(
                ChangeHistoryEntity(
                    id = existing?.id ?: 0,
                    date = today,
                    etfValue = etfValue,
                    stockValue = stockValue,
                    totalValue = totalValue
                )
            )
            AppLog.log("Change history recorded: ETF=${"%.2f".format(etfValue)}, Stock=${"%.2f".format(stockValue)}, Total=${"%.2f".format(totalValue)}")
        } catch (e: Exception) {
            AppLog.log("Change history record failed: ${e.message}")
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        accountRepository.getAllAccountsWithValues(),
        itemRepository.getAllItems(),
        _marketIndices
    ) { accounts, items, indices ->
        val tickerPositions = items
            .filter { it.value > 0 }
            .map { item ->
                TickerPosition(
                    ticker = item.ticker,
                    totalQuantity = item.quantity,
                    totalValue = item.value
                )
            }
            .sortedByDescending { it.totalValue }

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
            positions = tickerPositions,
            marketIndices = indices,
            topGainers = topGainers,
            topLosers = topLosers,
            overallDailyByType = overallByType
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
