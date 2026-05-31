package com.investhelp.app.ui.nextday

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.local.entity.InvestmentItemEntity
import com.investhelp.app.data.remote.StockPriceService
import com.investhelp.app.data.remote.TickerScanData
import com.investhelp.app.data.repository.InvestmentItemRepository
import com.investhelp.app.data.repository.TransactionRepository
import com.investhelp.app.model.InvestmentType
import com.investhelp.app.ui.settings.SettingsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

enum class NextDayAction(val label: String, val description: String) {
    STRONG_BUY("STRONG BUY", "High-volume breakout. Add to position at open."),
    TRIM_PROFIT("TRIM PROFITS", "Position has hit upper targets. Harvest gains."),
    REBALANCE_TRIM("REBALANCE", "Concentration cap breached. Trim to reduce risk exposure."),
    STOP_LOSS("STOP LOSS", "Technical breakdown. Exit or protect position immediately."),
    HOLD("HOLD", "Position stable and within normal operational parameters.")
}

data class ActionableSignal(
    val ticker: String,
    val type: InvestmentType,
    val shares: Double,
    val currentPrice: Double,
    val totalValue: Double,
    val allocationPct: Double,
    val costBasis: Double,
    val totalReturnPct: Double,
    val action: NextDayAction,
    val reasoning: String,
    val detailLog: String = ""
)

@HiltViewModel
class NextDayActionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val itemRepository: InvestmentItemRepository,
    private val transactionRepository: TransactionRepository,
    private val stockPriceService: StockPriceService
) : ViewModel() {

    private val _signals = MutableStateFlow<List<ActionableSignal>>(emptyList())
    val signals: StateFlow<List<ActionableSignal>> = _signals.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow("")
    val scanProgress: StateFlow<String> = _scanProgress.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun runScan() {
        val prefs = context.getSharedPreferences(SettingsViewModel.PREFS_NAME, Context.MODE_PRIVATE)
        val trailingStopPct = prefs.getInt(SettingsViewModel.KEY_TRAILING_STOP_PCT, SettingsViewModel.DEFAULT_TRAILING_STOP_PCT)
        val profitTargetPct = prefs.getInt(SettingsViewModel.KEY_PROFIT_TARGET_PCT, SettingsViewModel.DEFAULT_PROFIT_TARGET_PCT)
        val stockCap = prefs.getInt(SettingsViewModel.KEY_STOCK_CONCENTRATION_CAP, SettingsViewModel.DEFAULT_STOCK_CONCENTRATION_CAP)
        val etfCap = prefs.getInt(SettingsViewModel.KEY_ETF_CONCENTRATION_CAP, SettingsViewModel.DEFAULT_ETF_CONCENTRATION_CAP)

        viewModelScope.launch {
            _isScanning.value = true
            _error.value = null
            _signals.value = emptyList()

            try {
                val items = itemRepository.getAllItems().first()
                    .filter { it.quantity > 0 }
                if (items.isEmpty()) {
                    _error.value = "No positions with shares to scan."
                    return@launch
                }

                val totalPortfolioValue = items.sumOf { it.value }
                if (totalPortfolioValue <= 0) {
                    _error.value = "Portfolio value is zero."
                    return@launch
                }

                val results = mutableListOf<ActionableSignal>()

                for ((index, item) in items.withIndex()) {
                    _scanProgress.value = "Scanning ${item.ticker} (${index + 1}/${items.size})"

                    val costBasis = calculateCostBasis(item.ticker, item.currentPrice)
                    val scanData = stockPriceService.fetchScanData(item.ticker)
                    val allocationPct = (item.value / totalPortfolioValue) * 100.0
                    val totalReturnPct = if (costBasis > 0) ((item.currentPrice - costBasis) / costBasis) * 100.0 else 0.0

                    val signal = evaluatePosition(
                        item = item,
                        scanData = scanData,
                        allocationPct = allocationPct,
                        costBasis = costBasis,
                        totalReturnPct = totalReturnPct,
                        trailingStopPct = trailingStopPct,
                        profitTargetPct = profitTargetPct,
                        stockCap = stockCap,
                        etfCap = etfCap
                    )
                    results.add(signal)
                }

                _signals.value = results.sortedWith(
                    compareBy<ActionableSignal> { it.action.ordinal }
                        .thenByDescending { it.allocationPct }
                )
            } catch (e: Exception) {
                _error.value = "Scan failed: ${e.message}"
            } finally {
                _isScanning.value = false
                _scanProgress.value = ""
            }
        }
    }

    private suspend fun calculateCostBasis(ticker: String, fallbackPrice: Double): Double {
        return try {
            val transactions = transactionRepository.getTransactionsByTicker(ticker).first()
            val buys = transactions.filter { it.action.name == "Buy" }
            if (buys.isNotEmpty()) {
                buys.sumOf { it.pricePerShare * it.numberOfShares } / buys.sumOf { it.numberOfShares }
            } else {
                fallbackPrice
            }
        } catch (_: Exception) {
            fallbackPrice
        }
    }

    private fun evaluatePosition(
        item: InvestmentItemEntity,
        scanData: TickerScanData?,
        allocationPct: Double,
        costBasis: Double,
        totalReturnPct: Double,
        trailingStopPct: Int,
        profitTargetPct: Int,
        stockCap: Int,
        etfCap: Int
    ): ActionableSignal {
        val action: NextDayAction
        val reasoning: String
        val log = StringBuilder()

        log.appendLine("=== ${item.ticker} (${item.type.name}) ===")
        log.appendLine("Current Price: $${String.format(Locale.US, "%.2f", item.currentPrice)}")
        log.appendLine("Shares: ${String.format(Locale.US, "%.4f", item.quantity)}")
        log.appendLine("Position Value: $${String.format(Locale.US, "%.2f", item.value)}")
        log.appendLine("Cost Basis (avg buy price): $${String.format(Locale.US, "%.2f", costBasis)}")
        log.appendLine("Total Return: (${String.format(Locale.US, "%.2f", item.currentPrice)} - ${String.format(Locale.US, "%.2f", costBasis)}) / ${String.format(Locale.US, "%.2f", costBasis)} x 100 = ${String.format(Locale.US, "%+.2f", totalReturnPct)}%")
        log.appendLine("Allocation: ${String.format(Locale.US, "%.2f", item.value)} / portfolio total x 100 = ${String.format(Locale.US, "%.2f", allocationPct)}%")
        log.appendLine("")

        if (scanData != null) {
            log.appendLine("--- Yahoo Finance Scan Data ---")
            log.appendLine("20-Day SMA: $${String.format(Locale.US, "%.2f", scanData.twentyDaySma)}")
            log.appendLine("20-Day Avg Volume: ${String.format(Locale.US, "%,d", scanData.avgVolume20Day)}")
            log.appendLine("Today's Closing Volume: ${String.format(Locale.US, "%,d", scanData.closingVolume)}")
            val volumeRatio = if (scanData.avgVolume20Day > 0) scanData.closingVolume.toDouble() / scanData.avgVolume20Day else 0.0
            log.appendLine("Volume Ratio: ${String.format(Locale.US, "%,d", scanData.closingVolume)} / ${String.format(Locale.US, "%,d", scanData.avgVolume20Day)} = ${String.format(Locale.US, "%.2f", volumeRatio)}x")
            log.appendLine("")
            log.appendLine("--- Tier A: Risk Check ---")
            log.appendLine("Price vs 20-Day SMA: ${String.format(Locale.US, "%.2f", item.currentPrice)} ${if (item.currentPrice >= scanData.twentyDaySma) ">=" else "<"} ${String.format(Locale.US, "%.2f", scanData.twentyDaySma)} → ${if (item.currentPrice < scanData.twentyDaySma) "STOP LOSS TRIGGERED" else "OK"}")
            log.appendLine("Return ${String.format(Locale.US, "%+.1f", totalReturnPct)}% vs Target +$profitTargetPct% → ${if (totalReturnPct >= profitTargetPct) "TRIM PROFIT TRIGGERED" else "OK"}")
            log.appendLine("")
            log.appendLine("--- Tier B: Concentration Check ---")
            val cap = if (item.type == InvestmentType.ETF) etfCap else stockCap
            log.appendLine("Allocation ${String.format(Locale.US, "%.1f", allocationPct)}% vs ${item.type.name} Cap $cap% → ${if (allocationPct > cap) "REBALANCE TRIGGERED" else "OK"}")
            log.appendLine("")
            log.appendLine("--- Tier C: Momentum Check ---")
            log.appendLine("Volume Ratio ${String.format(Locale.US, "%.2f", volumeRatio)}x vs Threshold 1.50x → ${if (volumeRatio >= 1.5) "STRONG BUY TRIGGERED" else "OK"}")
        } else {
            log.appendLine("(Scan data unavailable — Yahoo Finance fetch failed)")
        }

        when {
            scanData != null && scanData.twentyDaySma > 0 && item.currentPrice < scanData.twentyDaySma -> {
                action = NextDayAction.STOP_LOSS
                reasoning = "Price ($${String.format(Locale.US, "%.2f", item.currentPrice)}) closed below 20-day SMA ($${String.format(Locale.US, "%.2f", scanData.twentyDaySma)})."
            }

            totalReturnPct >= profitTargetPct -> {
                action = NextDayAction.TRIM_PROFIT
                reasoning = "Return +${String.format(Locale.US, "%.1f", totalReturnPct)}% exceeds target of +$profitTargetPct%."
            }

            item.type == InvestmentType.Stock && allocationPct > stockCap -> {
                action = NextDayAction.REBALANCE_TRIM
                reasoning = "Stock allocation ${String.format(Locale.US, "%.1f", allocationPct)}% exceeds cap of $stockCap%."
            }

            item.type == InvestmentType.ETF && allocationPct > etfCap -> {
                action = NextDayAction.REBALANCE_TRIM
                reasoning = "ETF allocation ${String.format(Locale.US, "%.1f", allocationPct)}% exceeds cap of $etfCap%."
            }

            scanData != null && scanData.avgVolume20Day > 0 &&
                scanData.closingVolume.toDouble() / scanData.avgVolume20Day >= 1.5 -> {
                action = NextDayAction.STRONG_BUY
                val ratio = scanData.closingVolume.toDouble() / scanData.avgVolume20Day
                reasoning = "Volume spike! Closing volume was ${String.format(Locale.US, "%.1f", ratio)}x its 20-day average."
            }

            else -> {
                action = NextDayAction.HOLD
                reasoning = "Position is healthy. Within all thresholds."
            }
        }

        log.appendLine("")
        log.appendLine(">>> RESULT: ${action.label}")

        return ActionableSignal(
            ticker = item.ticker,
            type = item.type,
            shares = item.quantity,
            currentPrice = item.currentPrice,
            totalValue = item.value,
            allocationPct = allocationPct,
            costBasis = costBasis,
            totalReturnPct = totalReturnPct,
            action = action,
            reasoning = reasoning,
            detailLog = log.toString()
        )
    }
}
