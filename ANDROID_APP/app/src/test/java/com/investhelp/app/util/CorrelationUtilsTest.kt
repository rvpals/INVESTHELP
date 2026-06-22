package com.investhelp.app.util

import com.investhelp.app.data.remote.HistoricalPrice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrelationUtilsTest {

    private fun prices(vararg closes: Double, baseTs: Long = 1_000L): List<HistoricalPrice> =
        closes.mapIndexed { i, c -> HistoricalPrice(timestamp = baseTs + i, close = c) }

    // ── dailyReturns ──────────────────────────────────────────────────────────

    @Test
    fun `dailyReturns computes correct values`() {
        val returns = CorrelationUtils.dailyReturns(listOf(100.0, 105.0, 100.0))
        assertEquals(2, returns.size)
        assertEquals(0.05, returns[0], 1e-9)
        assertEquals(-5.0 / 105.0, returns[1], 1e-9)
    }

    @Test
    fun `dailyReturns with single price returns empty`() {
        assertTrue(CorrelationUtils.dailyReturns(listOf(100.0)).isEmpty())
    }

    @Test
    fun `dailyReturns with empty list returns empty`() {
        assertTrue(CorrelationUtils.dailyReturns(emptyList()).isEmpty())
    }

    // ── pearson ───────────────────────────────────────────────────────────────

    @Test
    fun `pearson of identical series is 1`() {
        val r = listOf(0.01, -0.02, 0.03, 0.01, -0.01)
        assertEquals(1.0, CorrelationUtils.pearson(r, r), 1e-9)
    }

    @Test
    fun `pearson of perfectly inverse series is -1`() {
        val a = listOf(0.01, 0.02, 0.03, 0.04, 0.05)
        val b = a.map { -it }
        assertEquals(-1.0, CorrelationUtils.pearson(a, b), 1e-9)
    }

    @Test
    fun `pearson of zero-variance series returns NaN`() {
        val a = listOf(0.01, 0.02, 0.03)
        val b = listOf(0.05, 0.05, 0.05)
        assertTrue(CorrelationUtils.pearson(a, b).isNaN())
    }

    @Test
    fun `pearson with fewer than 2 points returns NaN`() {
        assertTrue(CorrelationUtils.pearson(listOf(0.01), listOf(0.02)).isNaN())
        assertTrue(CorrelationUtils.pearson(emptyList(), emptyList()).isNaN())
    }

    @Test
    fun `pearson result is clamped to -1 to 1`() {
        val a = listOf(0.01, 0.02, 0.03, 0.04)
        val b = listOf(0.01, 0.02, 0.03, 0.04)
        val r = CorrelationUtils.pearson(a, b)
        assertTrue(r >= -1.0 && r <= 1.0)
    }

    // ── alignPriceSeries ─────────────────────────────────────────────────────

    @Test
    fun `alignPriceSeries inner-joins on timestamp`() {
        val a = prices(10.0, 11.0, 12.0, baseTs = 1L)
        val b = listOf(
            HistoricalPrice(timestamp = 1L, close = 20.0),
            HistoricalPrice(timestamp = 3L, close = 22.0)  // ts=2 missing
        )
        val (alignedA, alignedB) = CorrelationUtils.alignPriceSeries(a, b)
        assertEquals(2, alignedA.size)
        assertEquals(10.0, alignedA[0], 1e-9)
        assertEquals(12.0, alignedA[1], 1e-9)
        assertEquals(20.0, alignedB[0], 1e-9)
        assertEquals(22.0, alignedB[1], 1e-9)
    }

    @Test
    fun `alignPriceSeries with no common timestamps returns empty`() {
        val a = prices(10.0, baseTs = 1L)
        val b = prices(20.0, baseTs = 100L)
        val (alignedA, alignedB) = CorrelationUtils.alignPriceSeries(a, b)
        assertTrue(alignedA.isEmpty())
        assertTrue(alignedB.isEmpty())
    }

    // ── buildMatrix ───────────────────────────────────────────────────────────

    @Test
    fun `buildMatrix diagonal is 1`() {
        val prices = mapOf(
            "AAPL" to listOf(100.0, 102.0, 101.0, 105.0, 103.0),
            "MSFT" to listOf(200.0, 198.0, 202.0, 201.0, 205.0)
        )
        val result = CorrelationUtils.buildMatrix(prices, emptyList())
        assertEquals(1.0, result.matrix[0][0], 1e-9)
        assertEquals(1.0, result.matrix[1][1], 1e-9)
    }

    @Test
    fun `buildMatrix is symmetric`() {
        val prices = mapOf(
            "AAPL" to listOf(100.0, 102.0, 101.0, 105.0, 103.0),
            "MSFT" to listOf(200.0, 198.0, 202.0, 201.0, 205.0)
        )
        val result = CorrelationUtils.buildMatrix(prices, emptyList())
        assertEquals(result.matrix[0][1], result.matrix[1][0], 1e-9)
    }

    @Test
    fun `buildMatrix for perfectly correlated pair returns 1 off-diagonal`() {
        val closes = listOf(100.0, 105.0, 103.0, 108.0, 106.0)
        val prices = mapOf("A" to closes, "B" to closes)
        val result = CorrelationUtils.buildMatrix(prices, emptyList())
        assertEquals(1.0, result.matrix[0][1], 1e-9)
    }

    // ── averageCorrelation ────────────────────────────────────────────────────

    @Test
    fun `averageCorrelation for perfectly correlated pair returns 1`() {
        val closes = listOf(10.0, 11.0, 12.0, 13.0, 14.0)
        val result = CorrelationUtils.buildMatrix(
            mapOf("A" to closes, "B" to closes), emptyList()
        )
        assertEquals(1.0, CorrelationUtils.averageCorrelation(result), 1e-9)
    }

    @Test
    fun `averageCorrelation with single ticker returns NaN`() {
        val result = CorrelationUtils.buildMatrix(
            mapOf("A" to listOf(10.0, 11.0, 12.0)), emptyList()
        )
        // Single ticker: average of zero pairs is NaN
        assertTrue(CorrelationUtils.averageCorrelation(result).isNaN())
    }

    // ── mostCorrelatedPair ────────────────────────────────────────────────────

    @Test
    fun `mostCorrelatedPair finds highest correlation`() {
        val up   = listOf(10.0, 11.0, 12.0, 13.0, 14.0)
        val down = listOf(14.0, 13.0, 12.0, 11.0, 10.0)
        val flat = listOf(10.0, 10.0, 10.0, 10.0, 10.1)
        val result = CorrelationUtils.buildMatrix(
            mapOf("A" to up, "B" to up, "C" to down), emptyList()
        )
        val pair = CorrelationUtils.mostCorrelatedPair(result)
        assertTrue(pair != null)
        assertEquals("A", pair!!.tickerA)
        assertEquals("B", pair.tickerB)
        assertEquals(1.0, pair.value, 1e-6)
    }

    // ── mostDiversifyingTicker ────────────────────────────────────────────────

    @Test
    fun `mostDiversifyingTicker returns null for fewer than 2 tickers`() {
        val result = CorrelationUtils.buildMatrix(
            mapOf("A" to listOf(10.0, 11.0, 12.0)), emptyList()
        )
        assertNull(CorrelationUtils.mostDiversifyingTicker(result))
    }

    @Test
    fun `mostDiversifyingTicker picks lowest average correlation ticker`() {
        val up      = listOf(10.0, 11.0, 12.0, 13.0, 14.0)
        val inverse = up.map { 24.0 - it }  // perfectly inverse
        val result = CorrelationUtils.buildMatrix(
            mapOf("A" to up, "B" to inverse), emptyList()
        )
        // Both have avg corr = -1.0 (symmetric), so either can be "most diversifying"
        val diversify = CorrelationUtils.mostDiversifyingTicker(result)
        assertTrue(diversify != null)
        assertEquals(-1.0, diversify!!.second, 1e-6)
    }
}
