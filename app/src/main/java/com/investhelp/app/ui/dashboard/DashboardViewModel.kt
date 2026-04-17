package com.investhelp.app.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.remote.StockPriceService
import com.investhelp.app.data.repository.AccountRepository
import com.investhelp.app.data.repository.InvestmentItemRepository
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
import javax.inject.Inject

data class TickerPosition(
    val ticker: String,
    val totalQuantity: Double,
    val totalValue: Double
)

data class DailyGlanceItem(
    val ticker: String,
    val name: String,
    val dayGainLoss: Double,
    val dayGainLossPercent: Double
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
    private val stockPriceService: StockPriceService
) : ViewModel() {

    private val prefs = context.getSharedPreferences(
        SettingsViewModel.PREFS_NAME, Context.MODE_PRIVATE
    )

    companion object {
        const val KEY_PIN_MARKET_INDICES = "pin_card_market_indices"
        const val KEY_PIN_POSITIONS = "pin_card_positions"
        const val KEY_PIN_DAILY_GLANCE = "pin_card_daily_glance"
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _pinStates = MutableStateFlow(
        mapOf(
            KEY_PIN_MARKET_INDICES to prefs.getBoolean(KEY_PIN_MARKET_INDICES, false),
            KEY_PIN_POSITIONS to prefs.getBoolean(KEY_PIN_POSITIONS, false),
            KEY_PIN_DAILY_GLANCE to prefs.getBoolean(KEY_PIN_DAILY_GLANCE, false)
        )
    )
    val pinStates: StateFlow<Map<String, Boolean>> = _pinStates.asStateFlow()

    fun setPinState(key: String, pinned: Boolean) {
        prefs.edit().putBoolean(key, pinned).apply()
        _pinStates.value = _pinStates.value.toMutableMap().apply { put(key, pinned) }
    }

    private val _marketIndices = MutableStateFlow<List<MarketIndexQuote>>(emptyList())

    init {
        refreshMarketIndices()
    }

    fun refreshMarketIndices() {
        viewModelScope.launch {
            val enabledSymbols = prefs.getStringSet(
                SettingsViewModel.KEY_MARKET_INDICES, null
            ) ?: SettingsViewModel.DEFAULT_MARKET_INDICES

            val orderedSymbols = SettingsViewModel.AVAILABLE_MARKET_INDICES
                .filter { it.symbol in enabledSymbols }

            // Initialize with empty prices to show cards immediately
            _marketIndices.value = orderedSymbols.map {
                MarketIndexQuote(symbol = it.symbol, label = it.label)
            }

            // Fetch each quote
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
                val byTicker = allItems.groupBy { it.ticker }
                var successCount = 0
                var failCount = 0
                for ((ticker, rows) in byTicker) {
                    try {
                        val quote = stockPriceService.fetchQuote(ticker)
                        val resolvedName = quote.shortName ?: rows.first().name
                        for (row in rows) {
                            val newValue = quote.price * row.quantity
                            val dayChange = (quote.price - quote.previousClose) * row.quantity
                            itemRepository.upsertItem(
                                row.copy(
                                    name = resolvedName,
                                    currentPrice = quote.price,
                                    value = newValue,
                                    dayGainLoss = dayChange,
                                    totalGainLoss = newValue - row.cost
                                )
                            )
                        }
                        successCount++
                    } catch (e: Exception) {
                        failCount++
                        AppLog.log("Price fetch $ticker failed: ${e.message}")
                    }
                }
                AppLog.log("Portfolio refresh: $successCount tickers ok" +
                        if (failCount > 0) ", $failCount failed" else "")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        accountRepository.getAllAccountsWithValues(),
        itemRepository.getAllItems(),
        _marketIndices
    ) { accounts, items, indices ->
        val tickerPositions = items
            .groupBy { it.ticker }
            .map { (ticker, list) ->
                TickerPosition(
                    ticker = ticker,
                    totalQuantity = list.sumOf { it.quantity },
                    totalValue = list.sumOf { it.value }
                )
            }
            .filter { it.totalValue > 0 }
            .sortedByDescending { it.totalValue }

        // Daily glance: aggregate by ticker across all accounts
        val glanceByTicker = items
            .groupBy { it.ticker }
            .map { (ticker, list) ->
                val totalDayGL = list.sumOf { it.dayGainLoss }
                val totalValue = list.sumOf { it.value }
                val previousValue = totalValue - totalDayGL
                val pct = if (previousValue != 0.0) totalDayGL / previousValue * 100.0 else 0.0
                DailyGlanceItem(
                    ticker = ticker,
                    name = list.first().name,
                    dayGainLoss = totalDayGL,
                    dayGainLossPercent = pct
                )
            }
        val topGainers = glanceByTicker
            .filter { it.dayGainLoss > 0 }
            .sortedByDescending { it.dayGainLoss }
            .take(5)
        val topLosers = glanceByTicker
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
