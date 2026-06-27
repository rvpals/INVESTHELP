package com.investhelp.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sharpe_ratio_cache")
data class SharpeCacheEntity(
    @PrimaryKey val id: Int = 1,            // single-row table; always id = 1
    val riskFreeRate: Double,
    val lookbackDays: Int,
    val sharpeRatio: Double?,
    val annualizedReturn: Double,
    val annualizedVolatility: Double,
    val alignedTradingDays: Int,
    val meanDailyReturn: Double,
    val dailyRiskFreeRateUsed: Double,
    val calculationDate: String,            // ISO date "yyyy-MM-dd"
    val tickerDetailsJson: String,          // JSON array of TickerDetail objects
    val portfolioReturnSeriesJson: String,  // JSON array of {ts, r} objects
    val skippedTickersJson: String,         // JSON array of strings
    val skipReasonsJson: String,            // JSON object {ticker: reason}
    val insufficientDataReason: String?,
    val calculatedAt: Long                  // epoch seconds
)
