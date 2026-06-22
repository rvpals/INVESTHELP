package com.investhelp.app.util

import com.investhelp.app.data.remote.HistoricalPrice
import kotlin.math.sqrt

object CorrelationUtils {

    // Aligns two price series to their shared trading dates (inner join on timestamp).
    fun alignPriceSeries(
        a: List<HistoricalPrice>,
        b: List<HistoricalPrice>
    ): Pair<List<Double>, List<Double>> {
        val mapB = b.associate { it.timestamp to it.close }
        val paired = a.mapNotNull { pa ->
            val cb = mapB[pa.timestamp] ?: return@mapNotNull null
            pa.close to cb
        }
        return paired.map { it.first } to paired.map { it.second }
    }

    // dailyReturn[i] = (close[i] - close[i-1]) / close[i-1]
    fun dailyReturns(closes: List<Double>): List<Double> {
        if (closes.size < 2) return emptyList()
        return (1 until closes.size).map { i ->
            (closes[i] - closes[i - 1]) / closes[i - 1]
        }
    }

    // Pearson correlation (sample, n-1 denominator). Returns NaN on insufficient data or zero variance.
    fun pearson(returnsA: List<Double>, returnsB: List<Double>): Double {
        val n = minOf(returnsA.size, returnsB.size)
        if (n < 2) return Double.NaN
        val a = returnsA.take(n)
        val b = returnsB.take(n)
        val meanA = a.average()
        val meanB = b.average()
        val num = a.indices.sumOf { (a[it] - meanA) * (b[it] - meanB) }
        val stdA = sampleStdDev(a, meanA)
        val stdB = sampleStdDev(b, meanB)
        if (stdA == 0.0 || stdB == 0.0) return Double.NaN
        return (num / ((n - 1) * stdA * stdB)).coerceIn(-1.0, 1.0)
    }

    private fun sampleStdDev(values: List<Double>, mean: Double): Double {
        val n = values.size
        if (n < 2) return 0.0
        val variance = values.sumOf { (it - mean) * (it - mean) } / (n - 1)
        return sqrt(variance)
    }

    data class MatrixResult(
        val tickers: List<String>,
        val matrix: List<List<Double>>,            // N×N, symmetric, diagonal = 1.0
        val marketCorrelation: Map<String, Double>, // ticker → corr vs ^GSPC
        val failedTickers: List<String>,
        val calculatedAt: Long                      // epoch seconds
    )

    // Builds the full N×N matrix. priceMap values must already be date-aligned.
    fun buildMatrix(
        priceMap: Map<String, List<Double>>,
        marketPrices: List<Double>
    ): MatrixResult {
        val tickers = priceMap.keys.toList()
        val returnMap = priceMap.mapValues { dailyReturns(it.value) }
        val marketReturns = dailyReturns(marketPrices)
        val n = tickers.size

        val matrix = List(n) { i ->
            List(n) { j ->
                when {
                    i == j -> 1.0
                    i > j  -> -1.0  // placeholder; filled below by symmetry
                    else   -> pearson(returnMap[tickers[i]]!!, returnMap[tickers[j]]!!)
                }
            }
        }.let { raw ->
            // Make symmetric
            List(n) { i ->
                List(n) { j ->
                    if (i <= j) raw[i][j] else raw[j][i]
                }
            }
        }

        val marketCorrelation = if (marketReturns.size >= 2) {
            tickers.associateWith { ticker ->
                pearson(returnMap[ticker]!!, marketReturns)
            }
        } else emptyMap()

        return MatrixResult(
            tickers = tickers,
            matrix = matrix,
            marketCorrelation = marketCorrelation,
            failedTickers = emptyList(),
            calculatedAt = System.currentTimeMillis() / 1000L
        )
    }

    // Mean of all upper-triangle (non-diagonal) pairs.
    fun averageCorrelation(result: MatrixResult): Double {
        val n = result.tickers.size
        if (n < 2) return Double.NaN
        val values = mutableListOf<Double>()
        for (i in 0 until n) {
            for (j in (i + 1) until n) {
                val v = result.matrix[i][j]
                if (!v.isNaN()) values.add(v)
            }
        }
        return if (values.isEmpty()) Double.NaN else values.average()
    }

    data class PairStat(val tickerA: String, val tickerB: String, val value: Double)

    // Pair with the highest correlation in the upper triangle.
    fun mostCorrelatedPair(result: MatrixResult): PairStat? {
        val n = result.tickers.size
        var best: PairStat? = null
        for (i in 0 until n) {
            for (j in (i + 1) until n) {
                val v = result.matrix[i][j]
                if (v.isNaN()) continue
                if (best == null || v > best.value)
                    best = PairStat(result.tickers[i], result.tickers[j], v)
            }
        }
        return best
    }

    // Ticker with lowest average correlation to all others.
    fun mostDiversifyingTicker(result: MatrixResult): Pair<String, Double>? {
        val n = result.tickers.size
        if (n < 2) return null
        return result.tickers.mapIndexed { i, ticker ->
            val others = (0 until n).filter { it != i }
                .mapNotNull { j -> result.matrix[i][j].takeIf { !it.isNaN() } }
            ticker to if (others.isEmpty()) Double.NaN else others.average()
        }.minByOrNull { it.second }
    }
}
