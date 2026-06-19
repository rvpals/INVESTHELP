package com.investhelp.app.util

import kotlin.math.ln
import kotlin.math.sqrt

object VolatilityCalculator {

    data class Result(
        val annualizedVolPct: Double,
        val dailyStdDevPct: Double
    )

    /**
     * Computes annualized volatility and daily std dev from a list of closing prices.
     * Uses log returns and sample standard deviation (÷ n-1), annualized by √252.
     */
    fun compute(closes: List<Double>): Result {
        if (closes.size < 2) return Result(0.0, 0.0)
        val logReturns = closes.zipWithNext { a, b -> if (a > 0.0) ln(b / a) else 0.0 }
        val mean = logReturns.average()
        val variance = logReturns.sumOf { (it - mean) * (it - mean) } / (logReturns.size - 1)
        val dailyStdDev = sqrt(variance)
        return Result(
            dailyStdDevPct = dailyStdDev * 100.0,
            annualizedVolPct = dailyStdDev * sqrt(252.0) * 100.0
        )
    }

    /** Returns where [current] sits in [low]..[high] as a 0–100 percentage. */
    fun rangePositionPct(current: Double, low: Double, high: Double): Double {
        if (high <= low) return 50.0
        return ((current - low) / (high - low) * 100.0).coerceIn(0.0, 100.0)
    }

    fun volatilityLabel(annualizedPct: Double): String = when {
        annualizedPct < 15.0 -> "Low"
        annualizedPct < 30.0 -> "Moderate"
        annualizedPct < 60.0 -> "High"
        else -> "Very High"
    }
}
