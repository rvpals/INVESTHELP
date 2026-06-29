package com.investhelp.app.util

import java.time.LocalDate
import kotlin.math.sqrt

/**
 * Reconstructs a daily portfolio equity curve from transaction history × historical close prices,
 * then computes O(N) sliding-window rolling Sharpe Ratio for 30-day and 90-day windows.
 *
 * Unlike the static Sharpe Ratio (which weights by current holdings), this engine replays
 * actual buy/sell transactions to determine the shares held on each historical trading day.
 */
object RollingRiskEngine {

    const val WINDOW_30 = 30
    const val WINDOW_90 = 90
    private const val TRADING_DAYS_PER_YEAR = 252

    data class TransactionInput(
        val date: LocalDate,
        val ticker: String,
        val numberOfShares: Double,
        val isBuy: Boolean
    )

    data class RollingRiskPoint(
        val date: LocalDate,
        val portfolioValue: Double,
        val dailyReturn: Double,
        val rolling30SharpeRatio: Double?,   // null during the first 29-day warm-up
        val rolling90SharpeRatio: Double?    // null during the first 89-day warm-up
    )

    /**
     * Builds a daily equity curve by replaying transactions against historical close prices.
     *
     * - Dates are the union of all trading days from every ticker's price series,
     *   starting on or after the earliest transaction date.
     * - Holdings are updated by applying all transactions whose date <= the current trading day.
     * - Last-known-price carry-forward is used when a ticker has no price on a specific day.
     * - Days where portfolio value is 0 (before any buys, or all-cash) are omitted.
     */
    fun buildEquityCurve(
        transactions: List<TransactionInput>,
        pricesByTicker: Map<String, Map<LocalDate, Double>>
    ): List<Pair<LocalDate, Double>> {
        if (transactions.isEmpty() || pricesByTicker.isEmpty()) return emptyList()

        val sorted = transactions.sortedBy { it.date }
        val earliestDate = sorted.first().date

        val allDates = pricesByTicker.values
            .flatMap { it.keys }
            .filter { it >= earliestDate }
            .toSortedSet()
            .toList()

        val holdings = mutableMapOf<String, Double>()
        val lastKnownPrice = mutableMapOf<String, Double>()
        val curve = mutableListOf<Pair<LocalDate, Double>>()
        var txIdx = 0

        for (date in allDates) {
            // Apply all transactions up to and including this trading day
            while (txIdx < sorted.size && sorted[txIdx].date <= date) {
                val tx = sorted[txIdx++]
                val current = holdings[tx.ticker] ?: 0.0
                holdings[tx.ticker] = if (tx.isBuy) current + tx.numberOfShares
                                      else (current - tx.numberOfShares).coerceAtLeast(0.0)
            }

            // Carry-forward prices: update last known price when data is available
            for ((ticker, priceMap) in pricesByTicker) {
                priceMap[date]?.let { lastKnownPrice[ticker] = it }
            }

            val value = holdings.entries.sumOf { (ticker, shares) ->
                shares * (lastKnownPrice[ticker] ?: 0.0)
            }
            if (value > 0) curve.add(date to value)
        }

        return curve
    }

    /**
     * Computes rolling 30-day and 90-day annualized Sharpe Ratios from an equity curve.
     *
     * Formula per window: mean(excess) / stddev(excess) × √252
     * Excess return = daily return − (annualRiskFreeRate / 252)
     * Returns null for the first (window − 1) days (warm-up period).
     */
    fun compute(
        dailyEquityCurve: List<Pair<LocalDate, Double>>,
        riskFreeRateAnnual: Double
    ): List<RollingRiskPoint> {
        if (dailyEquityCurve.size < 2) return emptyList()

        val dailyRf = riskFreeRateAnnual / TRADING_DAYS_PER_YEAR
        val returnDates = mutableListOf<LocalDate>()
        val dailyReturns = mutableListOf<Double>()

        for (i in 1 until dailyEquityCurve.size) {
            val prev = dailyEquityCurve[i - 1].second
            val curr = dailyEquityCurve[i].second
            if (prev <= 0) continue
            returnDates.add(dailyEquityCurve[i].first)
            dailyReturns.add((curr - prev) / prev)
        }

        if (dailyReturns.isEmpty()) return emptyList()

        val excess = dailyReturns.map { it - dailyRf }
        val s30 = slidingWindowSharpe(excess, WINDOW_30)
        val s90 = slidingWindowSharpe(excess, WINDOW_90)
        val valueByDate = dailyEquityCurve.toMap()

        return returnDates.mapIndexed { i, date ->
            RollingRiskPoint(
                date = date,
                portfolioValue = valueByDate[date] ?: 0.0,
                dailyReturn = dailyReturns[i],
                rolling30SharpeRatio = s30[i],
                rolling90SharpeRatio = s90[i]
            )
        }
    }

    /**
     * O(N) sliding window annualized Sharpe.
     *
     * Maintains a running sum and sum-of-squares so each step is O(1),
     * avoiding the O(N²) recalculation of a naive window approach.
     */
    private fun slidingWindowSharpe(excessReturns: List<Double>, window: Int): List<Double?> {
        val result = ArrayList<Double?>(excessReturns.size)
        var sum = 0.0
        var sumSq = 0.0
        val deque = ArrayDeque<Double>(window)

        for (x in excessReturns) {
            if (deque.size == window) {
                val old = deque.removeFirst()
                sum -= old
                sumSq -= old * old
            }
            deque.addLast(x)
            sum += x
            sumSq += x * x

            if (deque.size < window) {
                result.add(null)
            } else {
                val mean = sum / window
                // Welford-equivalent via running sums: Var = (ΣxΣ − (Σx)²/n) / (n−1)
                val variance = (sumSq - sum * sum / window) / (window - 1)
                val std = if (variance > 1e-12) sqrt(variance) else 0.0
                result.add(if (std > 0) mean / std * sqrt(TRADING_DAYS_PER_YEAR.toDouble()) else null)
            }
        }

        return result
    }
}
