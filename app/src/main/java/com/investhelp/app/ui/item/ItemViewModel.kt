package com.investhelp.app.ui.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.AppLog
import com.investhelp.app.data.local.dao.InvestmentAccountDao
import com.investhelp.app.data.local.entity.InvestmentAccountEntity
import com.investhelp.app.data.local.entity.InvestmentItemEntity
import com.investhelp.app.data.local.entity.InvestmentTransactionEntity
import com.investhelp.app.data.remote.AnalysisInfo
import com.investhelp.app.data.remote.StockPriceService
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
    private val accountDao: InvestmentAccountDao
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
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Accounts (for forms) ---
    val accounts: StateFlow<List<InvestmentAccountEntity>> =
        accountDao.getAllAccounts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Selected item state (for detail screen) ---
    private val _selectedItem = MutableStateFlow<InvestmentItemEntity?>(null)
    val selectedItem: StateFlow<InvestmentItemEntity?> = _selectedItem.asStateFlow()

    private val _itemTransactions = MutableStateFlow<List<InvestmentTransactionEntity>>(emptyList())
    val itemTransactions: StateFlow<List<InvestmentTransactionEntity>> = _itemTransactions.asStateFlow()

    private val _statistics = MutableStateFlow(ItemStatistics(null, null, null, null, null, null))
    val statistics: StateFlow<ItemStatistics> = _statistics.asStateFlow()

    // --- Analysis info ---
    private val _analysisInfo = MutableStateFlow<AnalysisInfo?>(null)
    val analysisInfo: StateFlow<AnalysisInfo?> = _analysisInfo.asStateFlow()

    private val _isLoadingAnalysis = MutableStateFlow(false)
    val isLoadingAnalysis: StateFlow<Boolean> = _isLoadingAnalysis.asStateFlow()

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

    // --- Position refresh state ---
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
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

    // --- Load item by ticker (for detail screen) ---
    fun loadItem(ticker: String) {
        viewModelScope.launch {
            itemRepository.observeItemByTicker(ticker).collect { item ->
                _selectedItem.value = item
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
        cost: Double
    ) {
        viewModelScope.launch {
            val existing = itemRepository.getItemByTicker(ticker)
            val item = InvestmentItemEntity(
                ticker = ticker,
                name = name,
                type = type,
                currentPrice = currentPrice,
                quantity = quantity,
                cost = cost,
                dayGainLoss = existing?.dayGainLoss ?: 0.0,
                totalGainLoss = existing?.totalGainLoss ?: 0.0,
                value = existing?.value ?: (quantity * currentPrice),
                dayHigh = existing?.dayHigh ?: 0.0,
                dayLow = existing?.dayLow ?: 0.0
            )
            itemRepository.upsertItem(item)
        }
    }

    // --- Save position (from add form) ---
    fun savePosition(ticker: String, quantity: Double, cost: Double, type: InvestmentType? = null) {
        viewModelScope.launch {
            val existing = itemRepository.getItemByTicker(ticker)
            val resolvedType = type ?: existing?.type ?: InvestmentType.Stock
            val value = existing?.value ?: 0.0
            val dayGainLoss = existing?.dayGainLoss ?: 0.0
            val totalGainLoss = value - cost

            itemRepository.upsertItem(
                InvestmentItemEntity(
                    ticker = ticker,
                    name = existing?.name ?: ticker,
                    type = resolvedType,
                    currentPrice = existing?.currentPrice ?: 0.0,
                    quantity = quantity,
                    cost = cost,
                    dayGainLoss = dayGainLoss,
                    totalGainLoss = totalGainLoss,
                    value = value,
                    dayHigh = existing?.dayHigh ?: 0.0,
                    dayLow = existing?.dayLow ?: 0.0
                )
            )
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
                    totalGainLoss = newValue - item.cost,
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
                            totalGainLoss = newValue - item.cost,
                            dayHigh = quote.dayHigh,
                            dayLow = quote.dayLow
                        )
                    )
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
                            totalGainLoss = newValue - item.cost,
                            dayHigh = quote.dayHigh,
                            dayLow = quote.dayLow
                        )
                    )
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
