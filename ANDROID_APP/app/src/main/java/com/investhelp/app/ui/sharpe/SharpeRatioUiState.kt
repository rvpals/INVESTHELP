package com.investhelp.app.ui.sharpe

import com.investhelp.app.util.SharpeCalculator

sealed interface SharpeRatioUiState {
    /** Initial state before the first computation has started. */
    data object Idle : SharpeRatioUiState

    /** Fetching price history; [progressMessage] describes which ticker is in flight. */
    data class Loading(val progressMessage: String = "") : SharpeRatioUiState

    /**
     * Computation finished.
     *
     * [portfolioReturnSeries] contains (epoch-second timestamp, decimal daily return) pairs,
     * aligned across all valid tickers, for charting the return series.
     */
    data class Success(
        val result: SharpeCalculator.SharpeResult,
        val portfolioReturnSeries: List<Pair<Long, Double>>,
        val isFromCache: Boolean = false,
        val cachedAt: Long = 0L            // epoch seconds; 0 when not from cache
    ) : SharpeRatioUiState

    /** Computation failed and cannot be displayed. */
    data class Error(val message: String) : SharpeRatioUiState
}
