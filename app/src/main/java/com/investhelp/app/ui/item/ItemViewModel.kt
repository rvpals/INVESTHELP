package com.investhelp.app.ui.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.AppLog
import com.investhelp.app.data.local.dao.InvestmentAccountDao
import com.investhelp.app.data.local.entity.InvestmentAccountEntity
import com.investhelp.app.data.local.entity.InvestmentItemEntity
import com.investhelp.app.data.local.entity.InvestmentTransactionEntity
import com.investhelp.app.data.local.entity.DefinitionEntity
import com.investhelp.app.data.remote.AnalysisInfo
import com.investhelp.app.data.remote.HistoricalPrice
import com.investhelp.app.data.remote.StockPriceService
import com.investhelp.app.data.repository.DefinitionRepository
import com.investhelp.app.data.repository.InvestmentItemRepository
import com.investhelp.app.data.repository.TransactionRepository
import com.investhelp.app.model.InvestmentType
import com.investhelp.app.model.ItemStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ItemViewModel @Inject constructor(
    private val itemRepository: InvestmentItemRepository,
    private val transactionRepository: TransactionRepository,
    private val stockPriceService: StockPriceService,
    private val accountDao: InvestmentAccountDao,
    private val definitionRepository: DefinitionRepository
) : ViewModel() {

    // --- Refresh/update state ---
    private val _refreshingTickers = MutableStateFlow<Set<String>>(emptySet())
    val refreshingTickers: StateFlow<Set<String>> = _refreshingTickers.asStateFlow()

    private val _isRefreshingAll = MutableStateFlow(false)
    val isRefreshingAll: StateFlow<Boolean> = _isRefreshingAll.asStateFlow()

    private val _priceMessage = MutableStateFlow<String?>(null)
    val priceMessage: StateFlow<String?> = _priceMessage.asStateFlow()

    fun clearPriceMessage() {
        _priceMessage.value = null
    }

    // --- All items ---
    val allItems: StateFlow<List<InvestmentItemEntity>> =
        itemRepository.getAllItems()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // --- Accounts (for forms) ---
    val accounts: StateFlow<List<InvestmentAccountEntity>> =
        accountDao.getAllAccounts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Selected item state (for detail screen) ---
    private val _selectedItem = MutableStateFlow<InvestmentItemEntity?>(null)
    val selectedItem: StateFlow<InvestmentItemEntity?> = _selectedItem.asStateFlow()

    private val _itemLoaded = MutableStateFlow(false)
    val itemLoaded: StateFlow<Boolean> = _itemLoaded.asStateFlow()

    private val _itemTransactions = MutableStateFlow<List<InvestmentTransactionEntity>>(emptyList())
    val itemTransactions: StateFlow<List<InvestmentTransactionEntity>> = _itemTransactions.asStateFlow()

    private val _statistics = MutableStateFlow(ItemStatistics(null, null, null, null, null, null))
    val statistics: StateFlow<ItemStatistics> = _statistics.asStateFlow()

    // --- Definitions ---
    val definitions: StateFlow<List<DefinitionEntity>> =
        definitionRepository.getAllDefinitions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Investing Performance ---
    data class InvestingPerfPoint(
        val date: LocalDate,
        val price: Double,
        val isTransaction: Boolean,
        val isCurrentPrice: Boolean = false,
        val numberOfShares: Double = 0.0
    )

    private val _investingPerformance = MutableStateFlow<List<InvestingPerfPoint>>(emptyList())
    val investingPerformance: StateFlow<List<InvestingPerfPoint>> = _investingPerformance.asStateFlow()

    private val _isLoadingInvestingPerf = MutableStateFlow(false)
    val isLoadingInvestingPerf: StateFlow<Boolean> = _isLoadingInvestingPerf.asStateFlow()

    private val _investingPerfError = MutableStateFlow<String?>(null)
    val investingPerfError: StateFlow<String?> = _investingPerfError.asStateFlow()

    fun loadInvestingPerformance(ticker: String, currentPrice: Double? = null) {
        viewModelScope.launch {
            _isLoadingInvestingPerf.value = true
            _investingPerfError.value = null
            try {
                val transactions = transactionRepository.getTransactionsByTicker(ticker).first()
                    .sortedBy { it.date }
                if (transactions.isEmpty()) {
                    _investingPerformance.value = emptyList()
                    return@launch
                }

                val points = mutableListOf<InvestingPerfPoint>()
                for (tx in transactions) {
                    val txDate = tx.date
                    val dayBefore = txDate.minusDays(1)
                    val dayAfter = txDate.plusDays(1)

                    val period1 = dayBefore.atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond()
                    val period2 = dayAfter.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond()

                    try {
                        val history = stockPriceService.fetchPriceHistoryByPeriod(ticker, period1, period2, "1d")
                        val beforePrice = history.firstOrNull { hp ->
                            java.time.Instant.ofEpochSecond(hp.timestamp)
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDate() == dayBefore
                        }?.close
                        val afterPrice = history.firstOrNull { hp ->
                            java.time.Instant.ofEpochSecond(hp.timestamp)
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDate() == dayAfter
                        }?.close

                        if (beforePrice != null) {
                            points.add(InvestingPerfPoint(dayBefore, beforePrice, false))
                        }
                        points.add(InvestingPerfPoint(txDate, tx.pricePerShare, true, numberOfShares = tx.numberOfShares))
                        if (afterPrice != null) {
                            points.add(InvestingPerfPoint(dayAfter, afterPrice, false))
                        }
                    } catch (_: Exception) {
                        points.add(InvestingPerfPoint(txDate, tx.pricePerShare, true, numberOfShares = tx.numberOfShares))
                    }
                }

                if (currentPrice != null && currentPrice > 0.0) {
                    points.add(InvestingPerfPoint(LocalDate.now(), currentPrice, isTransaction = false, isCurrentPrice = true))
                }

                _investingPerformance.value = points.sortedBy { it.date }
            } catch (e: Exception) {
                _investingPerfError.value = "Failed to load performance: ${e.message}"
                _investingPerformance.value = emptyList()
            } finally {
                _isLoadingInvestingPerf.value = false
            }
        }
    }

    // --- Analysis info ---
    private val _analysisInfo = MutableStateFlow<AnalysisInfo?>(null)
    val analysisInfo: StateFlow<AnalysisInfo?> = _analysisInfo.asStateFlow()

    private val _isLoadingAnalysis = MutableStateFlow(false)
    val isLoadingAnalysis: StateFlow<Boolean> = _isLoadingAnalysis.asStateFlow()

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

    // --- Fetch price for form ---
    private val _fetchedPrice = MutableStateFlow<Double?>(null)
    val fetchedPrice: StateFlow<Double?> = _fetchedPrice.asStateFlow()

    private val _fetchedName = MutableStateFlow<String?>(null)
    val fetchedName: StateFlow<String?> = _fetchedName.asStateFlow()

    private val _fetchedDayHigh = MutableStateFlow<Double?>(null)
    val fetchedDayHigh: StateFlow<Double?> = _fetchedDayHigh.asStateFlow()

    private val _fetchedDayLow = MutableStateFlow<Double?>(null)
    val fetchedDayLow: StateFlow<Double?> = _fetchedDayLow.asStateFlow()

    private val _fetchedPreviousClose = MutableStateFlow<Double?>(null)
    val fetchedPreviousClose: StateFlow<Double?> = _fetchedPreviousClose.asStateFlow()

    private val _fetchedLogo = MutableStateFlow<ByteArray?>(null)
    val fetchedLogo: StateFlow<ByteArray?> = _fetchedLogo.asStateFlow()

    private val _fetchedType = MutableStateFlow<InvestmentType?>(null)
    val fetchedType: StateFlow<InvestmentType?> = _fetchedType.asStateFlow()

    fun fetchPriceForTicker(ticker: String) {
        viewModelScope.launch {
            try {
                val quote = stockPriceService.fetchQuote(ticker)
                _fetchedPrice.value = quote.price
                _fetchedName.value = quote.shortName
                _fetchedDayHigh.value = quote.dayHigh
                _fetchedDayLow.value = quote.dayLow
                _fetchedPreviousClose.value = quote.previousClose
                _fetchedType.value = when (quote.quoteType?.uppercase()) {
                    "ETF" -> InvestmentType.ETF
                    "MUTUALFUND" -> InvestmentType.MutualFund
                    "CRYPTOCURRENCY" -> InvestmentType.Crypto
                    else -> InvestmentType.Stock
                }
            } catch (_: Exception) {
                _fetchedPrice.value = null
                _fetchedName.value = null
                _fetchedDayHigh.value = null
                _fetchedDayLow.value = null
                _fetchedPreviousClose.value = null
                _fetchedType.value = null
            }
            try {
                _fetchedLogo.value = stockPriceService.fetchLogo(ticker)
            } catch (_: Exception) {
                _fetchedLogo.value = null
            }
        }
    }

    fun clearFetchedPrice() {
        _fetchedPrice.value = null
        _fetchedName.value = null
        _fetchedDayHigh.value = null
        _fetchedDayLow.value = null
        _fetchedPreviousClose.value = null
        _fetchedLogo.value = null
        _fetchedType.value = null
    }

    // --- Position refresh state ---
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    // --- Full Yahoo Report ---
    private val _fullReport = MutableStateFlow<List<com.investhelp.app.data.remote.YahooReportSection>>(emptyList())
    val fullReport: StateFlow<List<com.investhelp.app.data.remote.YahooReportSection>> = _fullReport.asStateFlow()

    private val _isLoadingReport = MutableStateFlow(false)
    val isLoadingReport: StateFlow<Boolean> = _isLoadingReport.asStateFlow()

    fun fetchFullReport(ticker: String) {
        viewModelScope.launch {
            _isLoadingReport.value = true
            try {
                _fullReport.value = stockPriceService.fetchFullReport(ticker)
            } catch (_: Exception) {
                _fullReport.value = emptyList()
            } finally {
                _isLoadingReport.value = false
            }
        }
    }

    // --- News ---
    private val _newsArticles = MutableStateFlow<List<com.investhelp.app.data.remote.NewsArticle>>(emptyList())
    val newsArticles: StateFlow<List<com.investhelp.app.data.remote.NewsArticle>> = _newsArticles.asStateFlow()

    private val _isLoadingNews = MutableStateFlow(false)
    val isLoadingNews: StateFlow<Boolean> = _isLoadingNews.asStateFlow()

    fun fetchNews(ticker: String, count: Int = 5) {
        viewModelScope.launch {
            _isLoadingNews.value = true
            try {
                _newsArticles.value = stockPriceService.fetchNews(ticker, count)
            } catch (_: Exception) {
                _newsArticles.value = emptyList()
            } finally {
                _isLoadingNews.value = false
            }
        }
    }

    // --- Analysis ---
    fun fetchAnalysisInfo(ticker: String) {
        viewModelScope.launch {
            _isLoadingAnalysis.value = true
            _analysisError.value = null
            _analysisInfo.value = null
            try {
                _analysisInfo.value = stockPriceService.fetchAnalysisInfo(ticker)
            } catch (e: Exception) {
                _analysisError.value = "Failed to fetch analysis: ${e.message}"
            } finally {
                _isLoadingAnalysis.value = false
            }
        }
    }

    fun clearAnalysisInfo() {
        _analysisInfo.value = null
        _analysisError.value = null
    }

    // --- Price History ---
    private val _priceHistory = MutableStateFlow<List<HistoricalPrice>>(emptyList())
    val priceHistory: StateFlow<List<HistoricalPrice>> = _priceHistory.asStateFlow()

    private val _isLoadingPriceHistory = MutableStateFlow(false)
    val isLoadingPriceHistory: StateFlow<Boolean> = _isLoadingPriceHistory.asStateFlow()

    private val _priceHistoryError = MutableStateFlow<String?>(null)
    val priceHistoryError: StateFlow<String?> = _priceHistoryError.asStateFlow()

    fun loadPriceHistory(ticker: String, timeframe: String, hourlyInterval: String = "1h") {
        val (range, interval) = when (timeframe) {
            "Hourly" -> "1d" to hourlyInterval
            "Daily" -> "60d" to "1d"
            "Monthly" -> "13mo" to "1mo"
            "Yearly" -> "15y" to "1mo"
            else -> "60d" to "1d"
        }
        viewModelScope.launch {
            _isLoadingPriceHistory.value = true
            _priceHistoryError.value = null
            try {
                _priceHistory.value = stockPriceService.fetchPriceHistory(ticker, range, interval)
            } catch (e: Exception) {
                _priceHistoryError.value = "Failed to fetch price history: ${e.message}"
                _priceHistory.value = emptyList()
            } finally {
                _isLoadingPriceHistory.value = false
            }
        }
    }

    // --- Load item by ticker (for detail screen) ---
    fun loadItem(ticker: String) {
        _itemLoaded.value = false
        viewModelScope.launch {
            itemRepository.observeItemByTicker(ticker).collect { item ->
                _selectedItem.value = item
                _itemLoaded.value = true
            }
        }
        viewModelScope.launch {
            transactionRepository.getTransactionsByTicker(ticker).collect { transactions ->
                _itemTransactions.value = transactions
            }
        }
    }

    // --- Statistics ---
    fun loadStatistics(ticker: String, startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch {
            _statistics.value = itemRepository.getItemStatistics(ticker, startDate, endDate)
        }
    }

    // --- Save item (add or edit metadata) ---
    fun saveItem(
        ticker: String,
        name: String,
        type: InvestmentType,
        currentPrice: Double,
        quantity: Double,
        dayHigh: Double? = null,
        dayLow: Double? = null,
        previousClose: Double? = null,
        logo: ByteArray? = null
    ) {
        viewModelScope.launch {
            val existing = itemRepository.getItemByTicker(ticker)
            val resolvedDayHigh = dayHigh ?: existing?.dayHigh ?: 0.0
            val resolvedDayLow = dayLow ?: existing?.dayLow ?: 0.0
            val dayChange = if (previousClose != null && previousClose > 0.0)
                (currentPrice - previousClose) * quantity
            else
                existing?.dayGainLoss ?: 0.0
            val item = InvestmentItemEntity(
                ticker = ticker,
                name = name,
                type = type,
                currentPrice = currentPrice,
                quantity = quantity,
                dayGainLoss = dayChange,
                value = quantity * currentPrice,
                dayHigh = resolvedDayHigh,
                dayLow = resolvedDayLow,
                logo = logo ?: existing?.logo
            )
            itemRepository.upsertItem(item)
        }
    }

    // --- Save position (from add form) ---
    fun savePosition(ticker: String, quantity: Double, type: InvestmentType? = null) {
        viewModelScope.launch {
            val existing = itemRepository.getItemByTicker(ticker)
            val resolvedType = type ?: existing?.type ?: InvestmentType.Stock

            itemRepository.upsertItem(
                InvestmentItemEntity(
                    ticker = ticker,
                    name = existing?.name ?: ticker,
                    type = resolvedType,
                    currentPrice = existing?.currentPrice ?: 0.0,
                    quantity = quantity,
                    dayGainLoss = existing?.dayGainLoss ?: 0.0,
                    value = existing?.value ?: 0.0,
                    dayHigh = existing?.dayHigh ?: 0.0,
                    dayLow = existing?.dayLow ?: 0.0
                )
            )
        }
    }

    // --- Fetch missing logos in background ---
    fun fetchMissingLogos() {
        viewModelScope.launch {
            val items = itemRepository.getAllItems().first()
            for (item in items) {
                if (item.logo == null) {
                    stockPriceService.fetchLogo(item.ticker)?.let { logo ->
                        itemRepository.updateLogoByTicker(item.ticker, logo)
                    }
                }
            }
        }
    }

    fun deleteTransaction(transaction: InvestmentTransactionEntity) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }

    // --- Delete item ---
    fun deleteItem(ticker: String) {
        viewModelScope.launch {
            itemRepository.deleteByTicker(ticker)
        }
    }

    // --- Refresh price for a single ticker ---
    fun refreshPrice(ticker: String) {
        if (ticker.isBlank()) return
        viewModelScope.launch {
            _refreshingTickers.value = _refreshingTickers.value + ticker
            try {
                val quote = stockPriceService.fetchQuote(ticker)
                val item = itemRepository.getItemByTicker(ticker) ?: return@launch
                val newValue = item.quantity * quote.price
                val dayChange = (quote.price - quote.previousClose) * item.quantity
                itemRepository.upsertItem(item.copy(
                    currentPrice = quote.price,
                    value = newValue,
                    dayGainLoss = dayChange,
                    dayHigh = quote.dayHigh,
                    dayLow = quote.dayLow
                ))
            } catch (e: Exception) {
                _priceMessage.value = "Failed to fetch $ticker: ${e.message}"
                AppLog.log("Fetch $ticker failed: ${e.message}")
            } finally {
                _refreshingTickers.value = _refreshingTickers.value - ticker
            }
        }
    }

    // --- Refresh all positions with live quotes ---
    fun refreshAllPositions() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val allPositions = itemRepository.getAllItems().first()
            var successCount = 0
            var failCount = 0

            for (item in allPositions) {
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
                            dayHigh = quote.dayHigh,
                            dayLow = quote.dayLow
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
                    AppLog.log("Refresh ${item.ticker} failed: ${e.message}")
                }
            }

            val msg = "Refreshed $successCount tickers" +
                    if (failCount > 0) ", $failCount failed" else ""
            _message.value = msg
            AppLog.log(msg)
            _isRefreshing.value = false
        }
    }

    // --- Refresh all prices (simpler, just updates price) ---
    fun refreshAllPrices() {
        viewModelScope.launch {
            _isRefreshingAll.value = true
            val allPositions = itemRepository.getAllItems().first()
            if (allPositions.isEmpty()) {
                _priceMessage.value = "No items to refresh."
                _isRefreshingAll.value = false
                return@launch
            }
            var successCount = 0
            var failCount = 0
            for (item in allPositions) {
                _refreshingTickers.value = _refreshingTickers.value + item.ticker
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
                            dayHigh = quote.dayHigh,
                            dayLow = quote.dayLow
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
                    AppLog.log("Update ${item.ticker} failed: ${e.message}")
                } finally {
                    _refreshingTickers.value = _refreshingTickers.value - item.ticker
                }
            }
            val msg = "Updated $successCount tickers" +
                    if (failCount > 0) ", $failCount failed" else ""
            _priceMessage.value = msg
            AppLog.log(msg)
            _isRefreshingAll.value = false
        }
    }
}
