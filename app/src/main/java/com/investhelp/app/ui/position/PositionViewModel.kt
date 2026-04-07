package com.investhelp.app.ui.position

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.local.dao.InvestmentAccountDao
import com.investhelp.app.data.local.dao.InvestmentItemDao
import com.investhelp.app.data.local.dao.PositionDao
import com.investhelp.app.data.local.entity.InvestmentAccountEntity
import com.investhelp.app.data.local.entity.InvestmentItemEntity
import com.investhelp.app.data.local.entity.PositionEntity
import com.investhelp.app.data.remote.StockPriceService
import com.investhelp.app.model.InvestmentType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PositionViewModel @Inject constructor(
    private val positionDao: PositionDao,
    private val itemDao: InvestmentItemDao,
    private val accountDao: InvestmentAccountDao,
    private val stockPriceService: StockPriceService
) : ViewModel() {

    val positions: StateFlow<List<PositionEntity>> =
        positionDao.getAllPositions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableItems: StateFlow<List<InvestmentItemEntity>> =
        itemDao.getAllItems()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<InvestmentAccountEntity>> =
        accountDao.getAllAccounts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    private suspend fun getDefaultAccountId(): Long {
        val allAccounts = accountDao.getAllAccounts().first()
        return allAccounts.firstOrNull()?.id
            ?: throw IllegalStateException("No accounts exist. Create an account first.")
    }

    fun savePosition(ticker: String, quantity: Double, cost: Double, accountId: Long?) {
        viewModelScope.launch {
            // Auto-create Item if no item has this ticker
            val allItems = itemDao.getAllItems().first()
            val hasItem = allItems.any { it.ticker.equals(ticker, ignoreCase = true) }
            if (!hasItem) {
                itemDao.insertItem(
                    InvestmentItemEntity(
                        name = ticker,
                        ticker = ticker,
                        type = InvestmentType.Stock,
                        currentPrice = 0.0
                    )
                )
            }

            val resolvedAccountId = accountId ?: getDefaultAccountId()

            val existing = positionDao.getPosition(ticker, resolvedAccountId)
            val value = existing?.value ?: 0.0
            val dayGainLoss = existing?.dayGainLoss ?: 0.0
            val totalGainLoss = value - cost

            positionDao.upsertPosition(
                PositionEntity(
                    ticker = ticker,
                    accountId = resolvedAccountId,
                    quantity = quantity,
                    cost = cost,
                    dayGainLoss = dayGainLoss,
                    totalGainLoss = totalGainLoss,
                    value = value
                )
            )
        }
    }

    fun deletePosition(ticker: String, accountId: Long) {
        viewModelScope.launch {
            positionDao.deletePosition(ticker, accountId)
        }
    }

    fun refreshAllPositions() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val allPositions = positionDao.getAllPositions().first()
            var successCount = 0
            var failCount = 0

            for (pos in allPositions) {
                try {
                    val quote = stockPriceService.fetchQuote(pos.ticker)
                    val newValue = quote.price * pos.quantity
                    val dayChange = (quote.price - quote.previousClose) * pos.quantity
                    positionDao.upsertPosition(
                        pos.copy(
                            value = newValue,
                            dayGainLoss = dayChange,
                            totalGainLoss = newValue - pos.cost
                        )
                    )
                    successCount++
                } catch (_: Exception) {
                    failCount++
                }
            }

            _message.value = "Refreshed $successCount positions" +
                    if (failCount > 0) ", $failCount failed" else ""
            _isRefreshing.value = false
        }
    }
}
