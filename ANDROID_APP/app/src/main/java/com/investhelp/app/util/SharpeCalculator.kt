package com.investhelp.app.util

import com.investhelp.app.data.remote.HistoricalPrice
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Pure, stateless functions for computing the Sharpe Ratio of a portfolio.
 *
 * Intended call order for a full computation:
 *  1. alignPriceSeriesForAllTickers  — normalise to common trading days
 *  2. calculateDailyReturns          — per-ticker price differences
 *  3. calculatePortfolioWeights      — value-weighted allocations
 *  4. calculatePortfolioDailyReturns — weighted sum across all tickers
 *  5. annualizeReturn                — mean × 252
 *  6. calculateDailyRiskFreeRate     — annual rate / 252
 *  7. calculateExcessReturns         — portfolio return – risk-free rate
 *  8. annualizeStandardDeviation     — sample std dev × √252
 *  9. calculateSharpeRatio           — excess return / annualised volatility
 *
 * No Android, network, or database dependencies — all functions are unit-testable in pure JVM.
 */
object SharpeCalculator {

    /** Minimum aligned daily return observations required for a meaningful Sharpe Ratio. */
    const val MINIMUM_RETURN_OBSERVATIONS = 30

    /** Trading days per year used to annualise daily statistics. */
    const val TRADING_DAYS_PER_YEAR = 252

    // -------------------------------------------------------------------------
    // Result types
    // -------------------------------------------------------------------------

    /**
     * Per-ticker breakdown data used in the Calculation Detail card.
     * All values derived from the aligned price series — not stored in DB.
     */
    data class TickerDetail(
        val ticker: String,
        val shares: Double,
        val currentPrice: Double,
        val positionValue: Double,
        val weight: Double,            // 0.0–1.0
        val annualizedReturn: Double,
        val annualizedVolatility: Double,
        val tradingDays: Int
    )

    /**
     * Final structured output of a portfolio Sharpe computation.
     *
     * [sharpeRatio] is null when there is insufficient data or zero volatility.
     * [insufficientDataReason] explains the null when present.
     * [tickerDetails] is populated only on a full computation (not on insufficient-data early exit).
     */
    data class SharpeResult(
        val sharpeRatio: Double?,
        val annualizedReturn: Double,
        val annualizedVolatility: Double,
        val riskFreeRate: Double,
        val lookbackDays: Int,
        val calculationDate: LocalDate,
        val skippedTickers: List<String> = emptyList(),
        val skipReasons: Map<String, String> = emptyMap(),
        val insufficientDataReason: String? = null,
        val tickerDetails: List<TickerDetail> = emptyList(),
        val alignedTradingDays: Int = 0,
        val meanDailyReturn: Double = 0.0,
        val dailyRiskFreeRateUsed: Double = 0.0
    )

    /**
     * Price series for all tickers after alignment to their shared trading days.
     *
     * [timestamps] are epoch-seconds in chronological order, one entry per aligned day.
     * [pricesByTicker] closing prices correspond positionally to [timestamps].
     */
    data class AlignedSeries(
        val timestamps: List<Long>,
        val pricesByTicker: Map<String, List<Double>>
    )

    // -------------------------------------------------------------------------
    // Step 1 — align price series to shared trading days
    // -------------------------------------------------------------------------

    /**
     * Intersects all tickers on their shared trading-day timestamps and returns aligned
     * closing prices in chronological order.
     *
     * Tickers that have no overlap with the common set are reported via [onSkip] and
     * excluded from the returned series.
     */
    fun alignPriceSeriesForAllTickers(
        priceMap: Map<String, List<HistoricalPrice>>,
        onSkip: (ticker: String, reason: String) -> Unit = { _, _ -> }
    ): AlignedSeries {
        if (priceMap.isEmpty()) return AlignedSeries(emptyList(), emptyMap())

        val commonTimestamps = priceMap.values
            .map { prices -> prices.map { it.timestamp }.toSet() }
            .reduce { accumulator, set -> accumulator.intersect(set) }
            .sorted()

        if (commonTimestamps.isEmpty()) {
            priceMap.keys.forEach { ticker ->
                onSkip(ticker, "No overlapping trading days with other tickers")
            }
            return AlignedSeries(emptyList(), emptyMap())
        }

        val alignedPrices = priceMap.mapValues { (ticker, prices) ->
            val closeByTimestamp = prices.associate { it.timestamp to it.close }
            val aligned = commonTimestamps.mapNotNull { closeByTimestamp[it] }
            if (aligned.size < commonTimestamps.size) {
                onSkip(ticker, "Missing ${commonTimestamps.size - aligned.size} days in aligned period")
            }
            aligned
        }.filter { (ticker, aligned) ->
            // Remove any ticker that ended up with zero aligned prices after the mapNotNull above
            val keep = aligned.isNotEmpty()
            if (!keep) onSkip(ticker, "No prices survived alignment")
            keep
        }

        return AlignedSeries(timestamps = commonTimestamps, pricesByTicker = alignedPrices)
    }

    // -------------------------------------------------------------------------
    // Step 2 — daily returns
    // -------------------------------------------------------------------------

    /**
     * Computes daily percentage returns from a list of closing prices.
     * Formula: (close[i] – close[i-1]) / close[i-1]
     *
     * Delegates to [CorrelationUtils.dailyReturns] to avoid duplicate implementation.
     * Returns an empty list for fewer than 2 prices.
     */
    fun calculateDailyReturns(prices: List<Double>): List<Double> =
        CorrelationUtils.dailyReturns(prices)

    // -------------------------------------------------------------------------
    // Step 3 — portfolio weights
    // -------------------------------------------------------------------------

    /**
     * Computes each ticker's value-weighted fraction of total portfolio value.
     * Formula: weight_i = (shares_i × price_i) / Σ(shares_j × price_j)
     *
     * Tickers with zero or negative shares or price are excluded from the total.
     */
    fun calculatePortfolioWeights(
        holdings: Map<String, Double>,
        currentPrices: Map<String, Double>
    ): Map<String, Double> {
        val positionValues = holdings.mapNotNull { (ticker, shares) ->
            val price = currentPrices[ticker] ?: return@mapNotNull null
            if (shares <= 0.0 || price <= 0.0) return@mapNotNull null
            ticker to shares * price
        }.toMap()
        val total = positionValues.values.sum()
        if (total <= 0.0) return emptyMap()
        return positionValues.mapValues { (_, value) -> value / total }
    }

    // -------------------------------------------------------------------------
    // Step 4 — weighted portfolio daily returns
    // -------------------------------------------------------------------------

    /**
     * Computes the weighted-sum daily return for each trading day across all tickers.
     * Formula: portfolioReturn_d = Σ(weight_i × dailyReturn_i_d)
     *
     * All return series must be of equal length; call [alignPriceSeriesForAllTickers] first
     * to guarantee alignment before computing daily returns.
     */
    fun calculatePortfolioDailyReturns(
        tickerDailyReturns: Map<String, List<Double>>,
        weights: Map<String, Double>
    ): List<Double> {
        val activeReturns = tickerDailyReturns.filter { (ticker, _) -> weights.containsKey(ticker) }
        if (activeReturns.isEmpty()) return emptyList()
        val length = activeReturns.values.minOf { it.size }
        if (length == 0) return emptyList()
        return (0 until length).map { dayIndex ->
            activeReturns.entries.sumOf { (ticker, returns) ->
                (weights[ticker] ?: 0.0) * returns[dayIndex]
            }
        }
    }

    // -------------------------------------------------------------------------
    // Step 5 — annualise return
    // -------------------------------------------------------------------------

    /**
     * Annualises the mean daily return by scaling to a full trading year.
     * Formula: mean(dailyReturns) × 252
     */
    fun annualizeReturn(dailyReturns: List<Double>): Double {
        if (dailyReturns.isEmpty()) return 0.0
        return dailyReturns.average() * TRADING_DAYS_PER_YEAR
    }

    // -------------------------------------------------------------------------
    // Step 6 — daily risk-free rate
    // -------------------------------------------------------------------------

    /**
     * Converts an annualised risk-free rate to its per-day equivalent.
     * Formula: annualRate / 252
     */
    fun calculateDailyRiskFreeRate(annualRate: Double): Double =
        annualRate / TRADING_DAYS_PER_YEAR

    // -------------------------------------------------------------------------
    // Step 7 — excess returns
    // -------------------------------------------------------------------------

    /**
     * Subtracts the daily risk-free rate from each portfolio daily return.
     * Formula: excessReturn_d = portfolioReturn_d – dailyRiskFreeRate
     */
    fun calculateExcessReturns(
        portfolioDailyReturns: List<Double>,
        dailyRiskFreeRate: Double
    ): List<Double> = portfolioDailyReturns.map { it - dailyRiskFreeRate }

    // -------------------------------------------------------------------------
    // Step 8 — annualised standard deviation
    // -------------------------------------------------------------------------

    /**
     * Annualises the standard deviation of excess returns using the sample formula (÷ n-1).
     * Formula: stdDev(excessReturns, ddof=1) × √252
     *
     * Returns 0.0 for fewer than 2 observations; the caller must guard against division by zero
     * before passing this result to [calculateSharpeRatio].
     */
    fun annualizeStandardDeviation(excessReturns: List<Double>): Double {
        val n = excessReturns.size
        if (n < 2) return 0.0
        val mean = excessReturns.average()
        val variance = excessReturns.sumOf { (it - mean) * (it - mean) } / (n - 1)
        return sqrt(variance) * sqrt(TRADING_DAYS_PER_YEAR.toDouble())
    }

    // -------------------------------------------------------------------------
    // Step 9 — Sharpe Ratio
    // -------------------------------------------------------------------------

    /**
     * Computes the Sharpe Ratio rounded to 2 decimal places.
     * Formula: (annualizedReturn – riskFreeRate) / annualizedStandardDeviation
     *
     * Returns null when [annualizedStandardDeviation] is 0 to prevent division by zero.
     */
    fun calculateSharpeRatio(
        annualizedReturn: Double,
        riskFreeRate: Double,
        annualizedStandardDeviation: Double
    ): Double? {
        if (annualizedStandardDeviation == 0.0) return null
        val raw = (annualizedReturn - riskFreeRate) / annualizedStandardDeviation
        return BigDecimal(raw).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    // -------------------------------------------------------------------------
    // Interpretation
    // -------------------------------------------------------------------------

    /**
     * Returns a human-readable quality label for a given Sharpe Ratio.
     *
     * < 1.0   → "Subpar"
     * 1.0–2.0 → "Good"
     * 2.0–3.0 → "Very Good"
     * ≥ 3.0   → "Exceptional"
     */
    fun interpretSharpeRatio(sharpeRatio: Double): String = when {
        sharpeRatio < 1.0 -> "Subpar"
        sharpeRatio < 2.0 -> "Good"
        sharpeRatio < 3.0 -> "Very Good"
        else -> "Exceptional"
    }

    /**
     * Symmetric y-range for a daily-returns chart: whichever is larger of the absolute
     * max/min return or [minimumRange], ensuring the zero line is always centred.
     */
    fun chartYRange(returnsPercent: List<Double>, minimumRange: Double = 0.5): Double {
        if (returnsPercent.isEmpty()) return minimumRange
        val peak = returnsPercent.maxOfOrNull { abs(it) } ?: minimumRange
        return maxOf(peak, minimumRange)
    }
}
